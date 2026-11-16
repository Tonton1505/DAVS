package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DashboardViewDto(
        UUID id,
        UUID workbookId,
        String workbookName,
        String name,
        String description,
        String definition,
        Instant dataAsOf,
        boolean canView,
        boolean canInteract,
        boolean canFilter,
        boolean canDownload,
        boolean canComment,
        boolean canWebEdit,
        Map<String, Object> metadata
) {
}
