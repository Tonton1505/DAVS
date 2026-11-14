package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * External platform integration status DTO returned by {@code /api/v1/integrations} (UC25).
 * Encrypted OAuth tokens are never exposed in API responses (BUS-INT-02).
 */
public record IntegrationDto(
        String platform,
        String displayName,
        String description,
        String status,
        String scopes,
        String accountName,
        boolean hasToken,
        Instant tokenExpiresAt,
        Instant connectedAt,
        UUID connectedBy,
        Instant updatedAt
) {
}
