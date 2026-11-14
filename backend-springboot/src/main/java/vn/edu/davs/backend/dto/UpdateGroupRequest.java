package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.SiteRole;

/**
 * Request body for {@code PATCH /api/v1/groups/{id}} (UC04).
 */
public record UpdateGroupRequest(
        String name,
        String description,
        SiteRole minRole
) {
}
