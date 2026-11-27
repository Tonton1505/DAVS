package vn.edu.davs.backend.service;

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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import vn.edu.davs.backend.config.StorageProperties;
import vn.edu.davs.backend.enu.Bucket;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SettingKey;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.model.StoredObject;

/**
 * MinIO-backed object storage service with auto-bucket provisioning, filename sanitization,
 * magic-bytes verification, optional virus scanning, size limits from {@link SettingsService},
 * and automatic cross-platform local filesystem fallback when MinIO is offline in local dev (T-009).
 */
@Slf4j
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
     * Validates, scans (if enabled), and uploads an object to MinIO (or local fallback if MinIO is unreachable).
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
            byte[] payload = data.readAllBytes();
            byte[] header = payload.length <= HEADER_PEEK_BYTES
                    ? payload
                    : Arrays.copyOf(payload, HEADER_PEEK_BYTES);
            magicBytesValidator.validate(safeFileName, header);

            if (storageProperties.isVirusScanEnabled()) {
                virusScanner.scan(safeFileName, new ByteArrayInputStream(payload));
            }

            String objectKey = buildObjectKey(keyPrefix, safeFileName);
            long actualSize = sizeBytes > 0 ? sizeBytes : payload.length;

            try {
                ensureBucket(bucket);
                try (BufferedInputStream uploadStream = new BufferedInputStream(new ByteArrayInputStream(payload))) {
                    uploadStream(bucket, objectKey, resolvedContentType, uploadStream, actualSize);
                }
            } catch (Exception minioEx) {
                if (!isConnectionFailure(minioEx)) {
                    throw minioEx;
                }
                log.warn("MinIO is unreachable ({}); storing object in local fallback directory: {}",
                        minioEx.getMessage(), objectKey);
                writeLocalFallback(bucket, objectKey, payload);
            }

            return new StoredObject(bucket, objectKey, safeFileName, actualSize, resolvedContentType);
        } catch (DavsException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to upload object to MinIO", ex);
        }
    }

    /**
     * Opens a stream to read an object from MinIO (or local fallback). Caller must close the returned {@link InputStream}.
     */
    public InputStream get(Bucket bucket, String objectKey) {
        Objects.requireNonNull(bucket, "bucket must not be null");
        Objects.requireNonNull(objectKey, "objectKey must not be null");
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(resolveBucketName(bucket))
                            .object(objectKey)
                            .build()
            );
        } catch (Exception ex) {
            Path localPath = resolveLocalFallbackPath(bucket, objectKey);
            if (Files.isRegularFile(localPath)) {
                try {
                    return Files.newInputStream(localPath);
                } catch (IOException ioEx) {
                    throw new IllegalStateException("Failed to read local fallback object: " + objectKey, ioEx);
                }
            }
            throw new IllegalStateException("Failed to read object from MinIO: " + objectKey, ex);
        }
    }

    /**
     * Deletes an object from MinIO (and local fallback if present).
     */
    public void delete(Bucket bucket, String objectKey) {
        Objects.requireNonNull(bucket, "bucket must not be null");
        Objects.requireNonNull(objectKey, "objectKey must not be null");
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(resolveBucketName(bucket))
                            .object(objectKey)
                            .build()
            );
        } catch (Exception ex) {
            if (!isConnectionFailure(ex)) {
                throw new IllegalStateException("Failed to delete object from MinIO: " + objectKey, ex);
            }
        } finally {
            try {
                Files.deleteIfExists(resolveLocalFallbackPath(bucket, objectKey));
            } catch (IOException ignored) {
                // Ignore local cleanup error
            }
        }
    }

    /**
     * Checks whether an object exists in MinIO (or local fallback).
     */
    public boolean exists(Bucket bucket, String objectKey) {
        Objects.requireNonNull(bucket, "bucket must not be null");
        Objects.requireNonNull(objectKey, "objectKey must not be null");
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(resolveBucketName(bucket))
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (ErrorResponseException ex) {
            String code = ex.errorResponse() != null ? ex.errorResponse().code() : "";
            if ("NoSuchKey".equals(code) || "NoSuchBucket".equals(code)) {
                return Files.isRegularFile(resolveLocalFallbackPath(bucket, objectKey));
            }
            throw new IllegalStateException("Failed to stat object in MinIO: " + objectKey, ex);
        } catch (Exception ex) {
            if (isConnectionFailure(ex)) {
                return Files.isRegularFile(resolveLocalFallbackPath(bucket, objectKey));
            }
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
                            .bucket(resolveBucketName(bucket))
                            .object(objectKey)
                            .expiry(seconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate presigned URL for: " + objectKey, ex);
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
                        .bucket(resolveBucketName(bucket))
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
        String name = resolveBucketName(bucket);
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(name).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(name).build());
        }
        initializedBuckets.add(bucket);
    }

    private String resolveBucketName(Bucket bucket) {
        String prefix = storageProperties.getBucketPrefix();
        if (prefix == null || prefix.isBlank()) {
            return bucket.getBucketName();
        }
        return prefix.trim() + bucket.getBucketName();
    }

    private Path resolveLocalFallbackPath(Bucket bucket, String objectKey) {
        Path baseDir = Path.of(System.getProperty("java.io.tmpdir"), "davs-local-storage", resolveBucketName(bucket))
                .toAbsolutePath()
                .normalize();
        String safeRelative = objectKey.replace("\\", "/").replace("..", "").replaceAll("^/+", "");
        Path resolved = baseDir.resolve(safeRelative).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new DavsException(ErrorCode.ERR_PRJ_08);
        }
        return resolved;
    }

    private void writeLocalFallback(Bucket bucket, String objectKey, byte[] payload) throws IOException {
        Path target = resolveLocalFallbackPath(bucket, objectKey);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = Files.createTempFile(parent != null ? parent : target.getRoot(), "davs-", ".tmp");
        try {
            Files.write(temp, payload);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static boolean isConnectionFailure(Throwable throwable) {
        Throwable cur = throwable;
        while (cur != null) {
            if (cur instanceof ConnectException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
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
