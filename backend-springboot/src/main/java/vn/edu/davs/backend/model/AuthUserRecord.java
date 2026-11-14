package vn.edu.davs.backend.model;

import vn.edu.davs.backend.dto.AuthUserDto;

import vn.edu.davs.backend.enu.SiteRole;
import java.time.Instant;
import java.util.UUID;

/**
 * Internal persistence projection of a DAVS user row for authentication workflows.
 */
public record AuthUserRecord(
        UUID id,
        String email,
        String passwordHash,
        String fullName,
        SiteRole siteRole,
        String status,
        boolean mustChangePassword,
        Instant termsAcceptedAt
) {
    public boolean termsAccepted() {
        return termsAcceptedAt != null;
    }

    public AuthUserDto toDto() {
        return new AuthUserDto(
                id,
                email,
                fullName,
                siteRole,
                status,
                termsAccepted(),
                mustChangePassword
        );
    }
}
