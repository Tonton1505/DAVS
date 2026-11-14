package vn.edu.davs.backend.dto;

/**
 * OAuth 2.0 authorization redirect details returned by
 * {@code POST /api/v1/integrations/{platform}/connect} (UC25, BUS-INT-02).
 */
public record IntegrationConnectResponse(
        String platform,
        String authorizeUrl,
        String state,
        String scopes
) {
}
