package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Extract refresh run history item returned by UC14 endpoints.
 */
public record RefreshRunDto(
        UUID id,
        UUID datasourceId,
        UUID jobId,
        String refreshType,
        String triggerType,
        String status,
        long rowsProcessed,
        String errorCode,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {
}
