package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CustomViewDto(
        UUID id,
        UUID dashboardId,
        String name,
        Map<String, Object> filters,
        Map<String, Object> parameters,
        boolean isDefault,
        boolean isShared,
        UUID ownerId,
        String ownerName,
        Instant createdAt,
        Instant updatedAt
) {
}
