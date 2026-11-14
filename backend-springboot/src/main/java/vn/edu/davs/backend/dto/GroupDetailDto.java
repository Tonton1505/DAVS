package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.SiteRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Workgroup detail DTO including members returned by {@code GET /api/v1/groups/{id}} (UC04).
 */
public record GroupDetailDto(
        UUID id,
        String name,
        String description,
        SiteRole minRole,
        boolean isSystem,
        int memberCount,
        List<GroupMemberItem> members,
        Instant createdAt,
        Instant updatedAt
) {
    public GroupDetailDto {
        members = members == null ? List.of() : List.copyOf(members);
    }

    public record GroupMemberItem(
            UUID userId,
            String email,
            String fullName,
            SiteRole siteRole,
            String status,
            Instant joinedAt
    ) {
    }
}
