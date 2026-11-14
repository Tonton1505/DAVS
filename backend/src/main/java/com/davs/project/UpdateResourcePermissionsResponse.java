package com.davs.project;

import java.util.List;

/**
 * Response payload for {@code PUT /api/v1/resources/{type}/{id}/permissions}.
 */
public record UpdateResourcePermissionsResponse(
        ResourcePermissionsResponse permissions,
        List<String> warnings
) {
}
