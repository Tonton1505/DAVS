package com.davs.project;

import java.util.List;
import java.util.UUID;

/**
 * Response payload for {@code GET /api/v1/resources/{type}/{id}/permissions}.
 */
public record ResourcePermissionsResponse(
        String resourceType,
        UUID resourceId,
        String resourceName,
        String permissionMode,
        boolean lockedByParent,
        boolean canEdit,
        List<PermissionRuleDto> rules
) {
}
