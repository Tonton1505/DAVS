package com.davs.storage;

/**
 * Metadata of an object stored in MinIO.
 */
public record StoredObject(
        Bucket bucket,
        String objectKey,
        String sanitizedFileName,
        long sizeBytes,
        String contentType
) {
}
