package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.SiteRole;
import java.util.List;
import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/groups} (UC04).
 */
public record CreateGroupRequest(
        String name,
        String description,
        SiteRole minRole,
        List<UUID> userIds
) {
}
