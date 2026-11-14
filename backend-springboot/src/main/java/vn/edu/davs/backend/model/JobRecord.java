package vn.edu.davs.backend.model;

import vn.edu.davs.backend.enu.JobStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable snapshot of a row in the {@code jobs} table.
 */
public record JobRecord(
        UUID id,
        String type,
        String payloadJson,
        JobStatus status,
        Instant runAt,
        int attempts,
        int maxAttempts,
        String lastError,
        Instant lockedAt,
        String lockedBy,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy) {
}
