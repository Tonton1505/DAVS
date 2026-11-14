package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Data source DTO returned by UC11 &amp; UC14 endpoints.
 * Connection secrets/passwords are never returned (BUS-DAT-01).
 */
public record DatasourceDto(
        UUID id,
        String name,
        String description,
        String connectorType,
        String mode,
        String suggestedMode,
        String status,
        UUID ownerId,
        UUID projectId,
        String fileName,
        long rowCount,
        boolean certified,
        String qualityWarning,
        String sensitivity,
        int dependentWorkbooksCount,
        ConnectionSummaryDto connection,
        RefreshScheduleDto schedule,
        Instant lastRefreshedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public record ConnectionSummaryDto(
            String host,
            Integer port,
            String databaseName,
            String schemaName,
            String username,
            boolean hasPassword
    ) {
    }

    public record RefreshScheduleDto(
            String frequency,
            String runTime,
            String timezone,
            String refreshType,
            String incrementalColumn,
            String alertEmails,
            boolean enabled,
            boolean paused,
            int consecutiveFailures,
            Instant nextRunAt
    ) {
    }
}
