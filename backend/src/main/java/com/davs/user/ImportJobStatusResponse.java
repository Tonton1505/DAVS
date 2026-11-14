package com.davs.user;

import java.util.UUID;

/**
 * Status and summary counts for a CSV user import job ({@code GET /api/v1/users/import/{jobId}}).
 */
public record ImportJobStatusResponse(
        UUID jobId,
        String status,
        int totalRows,
        int createdCount,
        int skippedCount,
        int errorCount,
        boolean hasErrorReport
) {
}
