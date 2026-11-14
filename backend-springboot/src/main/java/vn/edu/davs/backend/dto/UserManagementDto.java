package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.SiteRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * User item returned by {@code GET /api/v1/users} (UC03).
 */
public record UserManagementDto(
        UUID id,
        String email,
        String fullName,
        SiteRole siteRole,
        String authMethod,
        String status,
        List<String> groups,
        Instant lastLoginAt,
        int ownedResourcesCount,
        Instant createdAt
) {
    public UserManagementDto {
        groups = groups == null ? List.of() : List.copyOf(groups);
    }
}
