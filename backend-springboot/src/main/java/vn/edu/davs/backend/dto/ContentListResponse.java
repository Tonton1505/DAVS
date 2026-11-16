package vn.edu.davs.backend.dto;

import java.util.List;
import java.util.UUID;

/**
 * Response envelope for {@code GET /api/v1/content} (UC06).
 */
public record ContentListResponse(
        UUID projectId,
        String projectName,
        List<ProjectDto> breadcrumbs,
        List<ContentItemDto> items,
        int totalCount
) {
}
