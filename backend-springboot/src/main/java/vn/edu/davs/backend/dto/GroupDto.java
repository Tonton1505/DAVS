package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.SiteRole;
import java.time.Instant;
import java.util.UUID;

/**
 * Workgroup summary DTO returned by {@code /api/v1/groups} (UC04).
 */
public record GroupDto(
        UUID id,
        String name,
        String description,
        SiteRole minRole,
        boolean isSystem,
        int memberCount,
        Instant createdAt,
        Instant updatedAt
) {
}
