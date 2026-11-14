package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.SiteRole;

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
