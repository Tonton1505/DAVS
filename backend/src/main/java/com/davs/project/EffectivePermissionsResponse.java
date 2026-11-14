package com.davs.project;

import com.davs.permission.Capability;
import java.util.Set;
import java.util.UUID;

/**
 * Response payload for {@code GET /api/v1/resources/{type}/{id}/permissions/effective}.
 */
public record EffectivePermissionsResponse(
        UUID userId,
        String displayName,
        String siteRole,
        String resourceType,
        UUID resourceId,
        boolean lockedByParent,
        Set<Capability> effectiveCapabilities
) {
}
