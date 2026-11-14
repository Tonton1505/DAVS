package vn.edu.davs.backend.dto;

/**
 * Response payload returned on successful login or token refresh.
 */
public record AuthTokenResponse(
        String accessToken,
        long expiresInSeconds,
        boolean requiresTermsAcceptance,
        boolean mustChangePassword,
        AuthUserDto user) {
}
