package com.davs.storage;

import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.common.settings.SettingsService;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * MinIO storage service handling bucket creation, file sanitization, magic-byte verification,
 * size-limit enforcement via {@link SettingsService}, and malware scanning via {@link VirusScanner}.
 */
@Service
public class StorageService {

    private static final long DEFAULT_MAX_UPLOAD_BYTES = 1_073_741_824L;
    private static final int HEADER_PEEK_BYTES = 4096;

    private static final byte[] ZIP_MAGIC = new byte[] {0x50, 0x4B, 0x03, 0x04};
    private static final byte[] OLE2_MAGIC = new byte[] {
        (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
    private static final byte[] PARQUET_MAGIC = new byte[] {0x50, 0x41, 0x52, 0x31};
    private static final byte[] PE_EXE_MAGIC = new byte[] {0x4D, 0x5A};
    private static final byte[] ELF_MAGIC = new byte[] {0x7F, 0x45, 0x4C, 0x46};

    private static final Set<String> TEXT_DATASET_EXTENSIONS = Set.of("csv", "tsv", "txt", "json");
    private static final Set<String> ALL_DATASET_EXTENSIONS = Set.of(
            "csv", "tsv", "txt", "json", "xlsx", "xls", "parquet"
    );

    private final MinioClient minioClient;
    private final SettingsService settingsService;
    private final VirusScanner virusScanner;
    private final Set<Bucket> verifiedBuckets = ConcurrentHashMap.newKeySet();

    public StorageService(
            MinioClient minioClient,
            SettingsService settingsService,
            VirusScanner virusScanner
    ) {
        this.minioClient = Objects.requireNonNull(minioClient, "minioClient must not be null");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService must not be null");
        this.virusScanner = Objects.requireNonNull(virusScanner, "virusScanner must not be null");
    }

    /**
     * Sanitizes a user-supplied file name by removing directory traversal segments and illegal characters.
     *
     * @param rawFileName raw file name from client
     * @return sanitized safe file name
     */
    public String sanitizeFileName(String rawFileName) {
        if (rawFileName == null || rawFileName.isBlank()) {
            return "unnamed";
        }
        String normalized = rawFileName.replace('\\', '/').replace("\u0000", "");
        int lastSlash = normalized.lastIndexOf('/');
        String baseName = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;

        String cleaned = baseName
                .replaceAll("\\.\\.+", ".")
                .replaceAll("[/\\\\:*?\"<>|\\p{Cntrl}]", "_")
                .trim();

        while (cleaned.startsWith(".")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.isEmpty()) {
            return "unnamed";
        }
        if (cleaned.length() > 128) {
            int dotIdx = cleaned.lastIndexOf('.');
            if (dotIdx > 0 && cleaned.length() - dotIdx <= 10) {
                String ext = cleaned.substring(dotIdx);
                cleaned = cleaned.substring(0, 128 - ext.length()) + ext;
            } else {
                cleaned = cleaned.substring(0, 128);
            }
        }
        return cleaned;
    }

    /**
     * Builds a unique object key for a workbook version per skill S7.
     */
    public String buildWorkbookKey(UUID workbookId, int version, String rawFileName) {
        String safeName = sanitizeFileName(rawFileName);
        return "workbooks/%s/v%d/%s".formatted(workbookId, version, safeName);
    }

    /**
     * Builds a unique object key with a random UUID prefix so user filenames are never used as unique keys.
     */
    public String buildObjectKey(String prefix, String rawFileName) {
        String safeName = sanitizeFileName(rawFileName);
        String cleanPrefix = prefix == null ? "items" : prefix.replaceAll("^/+|/+$", "");
        return "%s/%s/%s".formatted(cleanPrefix, UUID.randomUUID(), safeName);
    }

    /**
     * Ensures all standard DAVS buckets exist in MinIO, creating any missing buckets.
     */
    public void ensureAllBucketsExist() {
        for (Bucket bucket : Bucket.values()) {
            ensureBucketExists(bucket);
        }
    }

    /**
     * Ensures the given bucket exists in MinIO.
     */
    public void ensureBucketExists(Bucket bucket) {
        if (verifiedBuckets.contains(bucket)) {
            return;
        }
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket.getBucketName()).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket.getBucketName()).build()
                );
            }
            verifiedBuckets.add(bucket);
        } catch (Exception ex) {
            throw new DavsException(ErrorCode.ERR_SYS_01, ex, "minio-bucket-" + bucket.getBucketName());
        }
    }

    /**
     * Validates and uploads an object to MinIO.
     */
    public void put(
            Bucket bucket,
            String objectKey,
            InputStream inputStream,
            long size,
            String contentType
    ) {
        Objects.requireNonNull(bucket, "bucket must not be null");
        Objects.requireNonNull(objectKey, "objectKey must not be null");
        Objects.requireNonNull(inputStream, "inputStream must not be null");

        checkSizeLimit(size);

        String targetFileName = sanitizeFileName(objectKey);
        PushbackInputStream pushbackStream = new PushbackInputStream(inputStream, HEADER_PEEK_BYTES);
        byte[] headerBytes = peekHeader(pushbackStream);

        validateMagicBytesAndExtension(bucket, targetFileName, headerBytes);

        VirusScanner.ScanResult scanResult = virusScanner.scan(targetFileName, headerBytes, size);
        if (!scanResult.clean()) {
            throw new DavsException(ErrorCode.ERR_PRJ_08);
        }

        ensureBucketExists(bucket);
        String safeContentType = (contentType == null || contentType.isBlank())
                ? "application/octet-stream"
                : contentType;

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket.getBucketName())
                            .object(objectKey)
                            .stream(pushbackStream, size, -1)
                            .contentType(safeContentType)
                            .build()
            );
        } catch (Exception ex) {
            throw new DavsException(ErrorCode.ERR_SYS_01, ex, "minio-put");
        }
    }

    /**
     * Convenience overload to upload a byte array.
     */
    public void put(Bucket bucket, String objectKey, byte[] content, String contentType) {
        Objects.requireNonNull(content, "content must not be null");
        put(bucket, objectKey, new ByteArrayInputStream(content), content.length, contentType);
    }

    /**
     * Retrieves an object stream from MinIO.
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
            throw new DavsException(ErrorCode.ERR_SYS_01, ex, "minio-get");
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
            throw new DavsException(ErrorCode.ERR_SYS_01, ex, "minio-delete");
        }
    }

    /**
     * Checks whether an object exists in the given MinIO bucket.
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
            if ("NoSuchKey".equalsIgnoreCase(code)
                    || "NoSuchObject".equalsIgnoreCase(code)
                    || "NoSuchBucket".equalsIgnoreCase(code)) {
                return false;
            }
            throw new DavsException(ErrorCode.ERR_SYS_01, ex, "minio-stat");
        } catch (Exception ex) {
            throw new DavsException(ErrorCode.ERR_SYS_01, ex, "minio-stat");
        }
    }

    /**
     * Generates a short-lived presigned GET URL for downloading an object from MinIO.
     */
    public URL presignGet(Bucket bucket, String objectKey, Duration expiry) {
        Objects.requireNonNull(bucket, "bucket must not be null");
        Objects.requireNonNull(objectKey, "objectKey must not be null");
        Objects.requireNonNull(expiry, "expiry must not be null");
        int expirySeconds = (int) Math.max(1L, Math.min(expiry.toSeconds(), 604_800L));
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket.getBucketName())
                            .object(objectKey)
                            .expiry(expirySeconds)
                            .build()
            );
            return URI.create(url).toURL();
        } catch (Exception ex) {
            throw new DavsException(ErrorCode.ERR_SYS_01, ex, "minio-presign");
        }
    }

    private void checkSizeLimit(long size) {
        long maxBytes = settingsService.getLong(
                SettingsService.KEY_UPLOAD_MAX_BYTES,
                DEFAULT_MAX_UPLOAD_BYTES
        );
        if (size > maxBytes) {
            long maxMb = Math.max(1L, maxBytes / (1024L * 1024L));
            throw new DavsException(ErrorCode.ERR_PRJ_05, maxMb);
        }
    }

    private byte[] peekHeader(PushbackInputStream stream) {
        byte[] buffer = new byte[HEADER_PEEK_BYTES];
        int totalRead = 0;
        try {
            while (totalRead < HEADER_PEEK_BYTES) {
                int read = stream.read(buffer, totalRead, HEADER_PEEK_BYTES - totalRead);
                if (read == -1) {
                    break;
                }
                totalRead += read;
            }
            if (totalRead > 0) {
                stream.unread(buffer, 0, totalRead);
            }
        } catch (IOException ex) {
            throw new DavsException(ErrorCode.ERR_DAT_04, ex);
        }
        return Arrays.copyOf(buffer, totalRead);
    }

    private void validateMagicBytesAndExtension(Bucket bucket, String fileName, byte[] header) {
        String ext = extractExtension(fileName);
        switch (bucket) {
            case WORKBOOKS -> validateWorkbookFile(ext, header);
            case DATASETS -> validateDatasetFile(ext, header);
            default -> validateGenericSafeFile( header);
        }
    }

    private void validateWorkbookFile(String ext, byte[] header) {
        if (!"twb".equals(ext) && !"twbx".equals(ext)) {
            throw new DavsException(ErrorCode.ERR_PRJ_04);
        }
        if (header.length == 0 || startsWithBytes(header, PE_EXE_MAGIC) || startsWithBytes(header, ELF_MAGIC)) {
            throw new DavsException(ErrorCode.ERR_PRJ_04);
        }
        if ("twbx".equals(ext)) {
            if (!startsWithBytes(header, ZIP_MAGIC)) {
                throw new DavsException(ErrorCode.ERR_PRJ_04);
            }
        } else {
            if (startsWithBytes(header, ZIP_MAGIC) || containsNullByte(header)) {
                throw new DavsException(ErrorCode.ERR_PRJ_04);
            }
            String textHead = stripBomAndLeadingWhitespace(new String(header, StandardCharsets.UTF_8));
            if (!textHead.startsWith("<?xml") && !textHead.startsWith("<workbook")) {
                throw new DavsException(ErrorCode.ERR_PRJ_04);
            }
        }
    }

    private void validateDatasetFile(String ext, byte[] header) {
        if (!ALL_DATASET_EXTENSIONS.contains(ext) || header.length == 0) {
            throw new DavsException(ErrorCode.ERR_DAT_04);
        }
        if (startsWithBytes(header, PE_EXE_MAGIC) || startsWithBytes(header, ELF_MAGIC)) {
            throw new DavsException(ErrorCode.ERR_DAT_04);
        }
        if ("xlsx".equals(ext) && !startsWithBytes(header, ZIP_MAGIC)) {
            throw new DavsException(ErrorCode.ERR_DAT_04);
        }
        if ("xls".equals(ext) && !startsWithBytes(header, OLE2_MAGIC)) {
            throw new DavsException(ErrorCode.ERR_DAT_04);
        }
        if ("parquet".equals(ext) && !startsWithBytes(header, PARQUET_MAGIC)) {
            throw new DavsException(ErrorCode.ERR_DAT_04);
        }
        if (TEXT_DATASET_EXTENSIONS.contains(ext)) {
            if (startsWithBytes(header, ZIP_MAGIC)
                    || startsWithBytes(header, OLE2_MAGIC)
                    || containsNullByte(header)) {
                throw new DavsException(ErrorCode.ERR_DAT_04);
            }
        }
    }

    private void validateGenericSafeFile(byte[] header) {
        if (startsWithBytes(header, PE_EXE_MAGIC) || startsWithBytes(header, ELF_MAGIC)) {
            throw new DavsException(ErrorCode.ERR_PRJ_08);
        }
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private boolean startsWithBytes(byte[] content, byte[] prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean containsNullByte(byte[] content) {
        for (byte b : content) {
            if (b == 0) {
                return true;
            }
        }
        return false;
    }

    private String stripBomAndLeadingWhitespace(String input) {
        String s = input;
        if (s.startsWith("\uFEFF")) {
            s = s.substring(1);
        }
        return s.stripLeading();
    }
}
