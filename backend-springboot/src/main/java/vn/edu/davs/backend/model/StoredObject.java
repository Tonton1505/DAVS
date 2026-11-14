package vn.edu.davs.backend.model;

import vn.edu.davs.backend.enu.Bucket;

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
