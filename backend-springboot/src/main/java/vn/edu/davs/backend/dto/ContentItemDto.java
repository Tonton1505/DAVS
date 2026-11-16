package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Single item (sub-project or workbook) returned in {@code GET /api/v1/content} (UC06).
 */
public record ContentItemDto(
        UUID id,
        String resourceType,
        String name,
        String description,
        UUID projectId,
        UUID ownerId,
        String ownerName,
        String permissionMode,
        boolean isDefault,
        int currentVersion,
        int childCount,
        List<String> allowedActions,
        Instant updatedAt
) {
}
