package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.Capability;
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
