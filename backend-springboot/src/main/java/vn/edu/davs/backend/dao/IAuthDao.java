package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.model.AuthUserRecord;
import vn.edu.davs.backend.enu.SiteRole;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC persistence operations for UC01 authentication, lockout tracking, and refresh tokens.
 */
public interface IAuthDao {


    Optional<AuthUserRecord> findActiveUserByEmail(String email);


    Optional<AuthUserRecord> findActiveUserById(UUID userId);


    boolean hasValidLicensePool(SiteRole siteRole);


    Optional<LoginAttemptState> findLoginAttempt(String email, String ipAddress);


    LoginAttemptState recordFailure(
            String email,
            String ipAddress,
            int maxFailures,
            int lockMinutes,
            Instant now
    );


    boolean isMfaEnabled(UUID userId);


    Optional<String> findMfaSecretEncrypted(UUID userId);


    void savePendingMfaSecret(UUID userId, String encryptedSecret, Instant now);


    void enableMfa(UUID userId, Instant now);


    void disableMfa(UUID userId, Instant now);


    void linkOidcSubjectIfEmpty(UUID userId, String subject, Instant now);


    void saveOidcAuthState(String state, String codeVerifier, String redirectUri, Instant expiresAt);


    Optional<OidcAuthStateRecord> consumeOidcAuthState(String state, Instant now);


    public record OidcAuthStateRecord(
            String state,
            String codeVerifier,
            String redirectUri,
            Instant expiresAt
    ) {
    }


    void clearLoginAttempts(String email, String ipAddress);


    void markLoginTimestamps(UUID userId, Instant now);


    void acceptTermsAndActivate(UUID userId, Instant now);


    void saveRefreshToken(
            UUID userId,
            String tokenHash,
            boolean rememberMe,
            Instant expiresAt,
            Instant now
    );


    Optional<RefreshTokenRecord> findRefreshToken(String tokenHash);


    void revokeRefreshToken(String tokenHash, Instant now);


    public record LoginAttemptState(int failedCount, Instant lockedUntil) {
    }


    public record RefreshTokenRecord(
            UUID id,
            UUID userId,
            boolean rememberMe,
            Instant expiresAt,
            Instant revokedAt
    ) {
        public boolean isActive(Instant now) {
            return revokedAt == null && expiresAt.isAfter(now);
        }
    }
}
