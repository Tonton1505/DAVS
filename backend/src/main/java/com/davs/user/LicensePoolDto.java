package com.davs.user;

import com.davs.common.security.SiteRole;

/**
 * License quota summary for a site role.
 */
public record LicensePoolDto(
        SiteRole role,
        int total,
        int used,
        int available
) {
}
