package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.SiteRole;
import java.util.UUID;

/**
 * Public user profile returned by authentication endpoints.
 */
public record AuthUserDto(
        UUID id,
        String email,
        String fullName,
        SiteRole siteRole,
        String status,
        boolean termsAccepted,
        boolean mustChangePassword) {
}
