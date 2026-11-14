package com.davs.storage;

import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.common.service.SettingKey;
import com.davs.common.service.SettingsService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/**
 * MinIO-backed object storage service with auto-bucket provisioning, filename sanitization,
 * magic-bytes verification, optional virus scanning, and size limits from {@link SettingsService} (T-009).
 */
@Service
@RequiredArgsConstructor
public class StorageService {

    private static final int HEADER_PEEK_BYTES = 512;
    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final MinioClient minioClient;
    private final SettingsService settingsService;
    private final StorageProperties storageProperties;
    private final MagicBytesValidator magicBytesValidator;
    private final VirusScanner virusScanner;
    private final Set<Bucket> initializedBuckets = ConcurrentHashMap.newKeySet();

    /**
     * Validates, scans (if enabled), and uploads an object to MinIO.
     */
    public StoredObject put(
            Bucket bucket,
            String keyPrefix,
            String originalFileName,
            String contentType,
            InputStream data,
            long sizeBytes
    ) {
        Objects.requireNonNull(bucket, "bucket must not be null");
        Objects.requireNonNull(data, "data must not be null");

        long maxBytes = settingsService.getLong(SettingKey.UPLOAD_MAX_BYTES);
        if (sizeBytes < 0 || sizeBytes > maxBytes) {
            long maxMb = Math.max(1L, maxBytes / BYTES_PER_MB);
            throw new DavsException(ErrorCode.ERR_PRJ_05, maxMb);
        }

        String safeFileName = FileNameSanitizer.sanitize(originalFileName);
        String resolvedContentType = (contentType == null || contentType.isBlank())
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : contentType;

        try {
            BufferedInputStream buffered = new BufferedInputStream(data, HEADER_PEEK_BYTES * 2);
            buffered.mark(HEADER_PEEK_BYTES * 2);
            byte[] header = readHeaderBytes(buffered);
            magicBytesValidator.validate(safeFileName, header);
            buffered.reset();

            ensureBucket(bucket);
            String objectKey = buildObjectKey(keyPrefix, safeFileName);

            if (storageProperties.isVirusScanEnabled()) {
                uploadWithVirusScan(bucket, objectKey, safeFileName, resolvedContentType, buffered, sizeBytes);
            } else {
                uploadStream(bucket, objectKey, resolvedContentType, buffered, sizeBytes);
            }

            return new StoredObject(bucket, objectKey, safeFileName, sizeBytes, resolvedContentType);
        } catch (DavsException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to upload object to MinIO", ex);
        }
    }

    /**
     * Opens a stream to read an object from MinIO. Caller must close the returned {@link InputStream}.
     */
    public InputStream get(Bucket bucket, String objectKey) {
        Objects.requireNonNull(bucket, "bucket must not be null");
        Objects.requireNonNull(objectKey, "objectKey must not be null");
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket.getBucketName())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read object from MinIO: " + objectKey, ex);
        }
    }

    /**
     * Deletes an object from MinIO.
     */
    public void delete(Bucket bucket, String objectKey) {
        Objects.requireNonNull(bucket, "bucket must not be null");
        Objects.requireNonNull(objectKey, "objectKey must not be null");
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket.getBucketName())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to delete object from MinIO: " + objectKey, ex);
        }
    }

    /**
     * Checks whether an object exists in MinIO.
     */
    public boolean exists(Bucket bucket, String objectKey) {
        Objects.requireNonNull(bucket, "bucket must not be null");
        Objects.requireNonNull(objectKey, "objectKey must not be null");
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket.getBucketName())
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (ErrorResponseException ex) {
            String code = ex.errorResponse() != null ? ex.errorResponse().code() : "";
            if ("NoSuchKey".equals(code) || "NoSuchBucket".equals(code)) {
                return false;
            }
            throw new IllegalStateException("Failed to stat object in MinIO: " + objectKey, ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to stat object in MinIO: " + objectKey, ex);
        }
    }

    /**
     * Generates a time-limited presigned GET URL for downloading an object directly from MinIO.
     */
    public String presignGet(Bucket bucket, String objectKey, Duration expiry) {
        Objects.requireNonNull(bucket, "bucket must not be null");
        Objects.requireNonNull(objectKey, "objectKey must not be null");
        Duration effectiveExpiry = expiry != null
                ? expiry
                : Duration.ofMinutes(storageProperties.getPresignExpiryMinutes());
        int seconds = (int) Math.max(1L, effectiveExpiry.toSeconds());
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket.getBucketName())
                            .object(objectKey)
                            .expiry(seconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate presigned URL for: " + objectKey, ex);
        }
    }

    private void uploadWithVirusScan(
            Bucket bucket,
            String objectKey,
            String safeFileName,
            String contentType,
            InputStream stream,
            long sizeBytes
    ) throws Exception {
        Path tempFile = Files.createTempFile("davs-upload-", "-" + safeFileName);
        try {
            Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            try (InputStream scanStream = Files.newInputStream(tempFile)) {
                virusScanner.scan(safeFileName, scanStream);
            }
            long actualSize = sizeBytes > 0 ? sizeBytes : Files.size(tempFile);
            try (InputStream uploadStream = Files.newInputStream(tempFile)) {
                uploadStream(bucket, objectKey, contentType, uploadStream, actualSize);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void uploadStream(
            Bucket bucket,
            String objectKey,
            String contentType,
            InputStream stream,
            long sizeBytes
    ) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket.getBucketName())
                        .object(objectKey)
                        .stream(stream, sizeBytes, -1)
                        .contentType(contentType)
                        .build()
        );
    }

    private void ensureBucket(Bucket bucket) throws Exception {
        if (initializedBuckets.contains(bucket)) {
            return;
        }
        String name = bucket.getBucketName();
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(name).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(name).build());
        }
        initializedBuckets.add(bucket);
    }

    private static byte[] readHeaderBytes(BufferedInputStream stream) throws IOException {
        byte[] buffer = new byte[HEADER_PEEK_BYTES];
        int totalRead = 0;
        while (totalRead < HEADER_PEEK_BYTES) {
            int read = stream.read(buffer, totalRead, HEADER_PEEK_BYTES - totalRead);
            if (read < 0) {
                break;
            }
            totalRead += read;
        }
        return totalRead == buffer.length ? buffer : Arrays.copyOf(buffer, totalRead);
    }

    private static String buildObjectKey(String keyPrefix, String safeFileName) {
        String uniqueName = UUID.randomUUID() + "-" + safeFileName;
        if (keyPrefix == null || keyPrefix.isBlank()) {
            return uniqueName;
        }
        String cleanedPrefix = keyPrefix.replace("\\", "/")
                .replace("..", "")
                .replaceAll("^/+|/+$", "");
        if (cleanedPrefix.isBlank()) {
            return uniqueName;
        }
        return cleanedPrefix + "/" + uniqueName;
    }
}
