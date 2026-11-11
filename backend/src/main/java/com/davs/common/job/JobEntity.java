package com.davs.common.job;

import java.time.Instant;
import java.util.UUID;

public record JobEntity(
        UUID id,
        JobType type,
        String payload,
        JobStatus status,
        Instant runAt,
        int attempts,
        int maxAttempts,
        String lastError,
        Instant lockedAt,
        String lockedBy,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy
) {
}
