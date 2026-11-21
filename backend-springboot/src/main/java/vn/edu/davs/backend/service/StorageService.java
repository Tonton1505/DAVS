package vn.edu.davs.backend.service;

import vn.edu.davs.backend.enu.Bucket;
import vn.edu.davs.backend.model.StoredObject;
import java.io.InputStream;
import java.time.Duration;

/**
 * MinIO-backed object storage service with auto-bucket provisioning, filename sanitization,
 * magic-bytes verification, optional virus scanning, and size limits from {@link SettingsService} (T-009).
 */
public interface StorageService {


    /**
     * Validates, scans (if enabled), and uploads an object to MinIO.
     */
    StoredObject put(
            Bucket bucket,
            String keyPrefix,
            String originalFileName,
            String contentType,
            InputStream data,
            long sizeBytes
    );


    /**
     * Opens a stream to read an object from MinIO. Caller must close the returned {@link InputStream}.
     */
    InputStream get(Bucket bucket, String objectKey);


    /**
     * Deletes an object from MinIO.
     */
    void delete(Bucket bucket, String objectKey);


    /**
     * Checks whether an object exists in MinIO.
     */
    boolean exists(Bucket bucket, String objectKey);


    /**
     * Generates a time-limited presigned GET URL for downloading an object directly from MinIO.
     */
    String presignGet(Bucket bucket, String objectKey, Duration expiry);
}
