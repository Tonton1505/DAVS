package vn.edu.davs.backend.dto;

import java.util.List;
import java.util.UUID;

/**
 * Hierarchical project node returned by {@code GET /api/v1/projects/tree} (UC06).
 */
public record ProjectTreeNodeDto(
        UUID id,
        String name,
        String description,
        UUID parentId,
        UUID ownerId,
        String permissionMode,
        boolean isDefault,
        int depth,
        List<ProjectTreeNodeDto> children
) {
}
