package vn.edu.davs.backend.dto;

/**
 * Response payload returned on successful login, MFA challenge, or token refresh.
 */
public record AuthTokenResponse(
        String accessToken,
        long expiresInSeconds,
        boolean requiresTermsAcceptance,
        boolean mustChangePassword,
        AuthUserDto user,
        boolean mfaRequired,
        String mfaToken) {

    public AuthTokenResponse(
            String accessToken,
            long expiresInSeconds,
            boolean requiresTermsAcceptance,
            boolean mustChangePassword,
            AuthUserDto user) {
        this(accessToken, expiresInSeconds, requiresTermsAcceptance, mustChangePassword, user, false, null);
    }
}
