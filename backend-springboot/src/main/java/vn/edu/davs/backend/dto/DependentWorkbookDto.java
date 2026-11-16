package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Workbook dependent on a data source returned by {@code GET /api/v1/datasources/{id}/dependents} (UC14).
 */
public record DependentWorkbookDto(
        UUID id,
        String name,
        UUID projectId,
        UUID ownerId,
        String status,
        Instant updatedAt
) {
}
