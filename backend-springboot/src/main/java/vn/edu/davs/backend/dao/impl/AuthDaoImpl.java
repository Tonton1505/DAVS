package vn.edu.davs.backend.dao.impl;

import vn.edu.davs.backend.model.AuthUserRecord;
import vn.edu.davs.backend.enu.SiteRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.edu.davs.backend.dao.IAuthDao;

@RequiredArgsConstructor
@Repository
public class AuthDaoImpl implements IAuthDao {


    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<AuthUserRecord> findActiveUserByEmail(String email) {
        String sql = """
                SELECT id, email, password_hash, full_name, site_role, status,
                       must_change_password, terms_accepted_at
                FROM users
                WHERE lower(email) = lower(?) AND deleted_at IS NULL
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapUser(rs), email).stream().findFirst();
    }

    @Override
    public Optional<AuthUserRecord> findActiveUserById(UUID userId) {
        String sql = """
                SELECT id, email, password_hash, full_name, site_role, status,
                       must_change_password, terms_accepted_at
                FROM users
                WHERE id = ? AND deleted_at IS NULL
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapUser(rs), userId).stream().findFirst();
    }

    @Override
    public boolean hasValidLicensePool(SiteRole siteRole) {
        String sql = """
                SELECT COUNT(*)
                FROM license_pools
                WHERE role = ? AND total > 0
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, siteRole.name());
        return count != null && count > 0;
    }

    @Override
    public Optional<LoginAttemptState> findLoginAttempt(String email, String ipAddress) {
        String sql = """
                SELECT failed_count, locked_until
                FROM login_attempts
                WHERE lower(email) = lower(?) AND ip_address = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp lockedUntil = rs.getTimestamp("locked_until");
            return new LoginAttemptState(
                    rs.getInt("failed_count"),
                    lockedUntil != null ? lockedUntil.toInstant() : null
            );
        }, email, ipAddress).stream().findFirst();
    }

    @Override
    public LoginAttemptState recordFailure(
            String email,
            String ipAddress,
            int maxFailures,
            int lockMinutes,
            Instant now
    ) {
        Optional<LoginAttemptState> existing = findLoginAttempt(email, ipAddress);
        int previousCount = existing.map(LoginAttemptState::failedCount).orElse(0);
        int nextCount = previousCount + 1;
        Instant lockedUntil = null;
        if (nextCount >= maxFailures) {
            lockedUntil = now.plusSeconds(lockMinutes * 60L);
        }
        String normalizedEmail = email.toLowerCase();
        String sql = """
                INSERT INTO login_attempts (email, ip_address, failed_count, locked_until, last_failed_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (email, ip_address)
                DO UPDATE SET
                    failed_count = EXCLUDED.failed_count,
                    locked_until = EXCLUDED.locked_until,
                    last_failed_at = EXCLUDED.last_failed_at,
                    updated_at = EXCLUDED.updated_at
                """;
        Timestamp lockedTs = lockedUntil != null ? Timestamp.from(lockedUntil) : null;
        Timestamp nowTs = Timestamp.from(now);
        jdbcTemplate.update(sql, normalizedEmail, ipAddress, nextCount, lockedTs, nowTs, nowTs);
        return new LoginAttemptState(nextCount, lockedUntil);
    }

    @Override
    public boolean isMfaEnabled(UUID userId) {
        String sql = """
                SELECT mfa_enabled
                FROM users
                WHERE id = ? AND deleted_at IS NULL
                """;
        Boolean enabled = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getBoolean("mfa_enabled"), userId)
                .stream()
                .findFirst()
                .orElse(Boolean.FALSE);
        return Boolean.TRUE.equals(enabled);
    }

    @Override
    public Optional<String> findMfaSecretEncrypted(UUID userId) {
        String sql = """
                SELECT mfa_secret_encrypted
                FROM users
                WHERE id = ? AND deleted_at IS NULL
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("mfa_secret_encrypted"), userId)
                .stream()
                .filter(s -> s != null && !s.isBlank())
                .findFirst();
    }

    @Override
    public void savePendingMfaSecret(UUID userId, String encryptedSecret, Instant now) {
        String sql = """
                UPDATE users
                SET mfa_secret_encrypted = ?, updated_at = ?
                WHERE id = ? AND deleted_at IS NULL
                """;
        jdbcTemplate.update(sql, encryptedSecret, Timestamp.from(now), userId);
    }

    @Override
    public void enableMfa(UUID userId, Instant now) {
        String sql = """
                UPDATE users
                SET mfa_enabled = TRUE, mfa_verified_at = ?, updated_at = ?
                WHERE id = ? AND deleted_at IS NULL
                """;
        Timestamp ts = Timestamp.from(now);
        jdbcTemplate.update(sql, ts, ts, userId);
    }

    @Override
    public void disableMfa(UUID userId, Instant now) {
        String sql = """
                UPDATE users
                SET mfa_enabled = FALSE, mfa_secret_encrypted = NULL, mfa_verified_at = NULL, updated_at = ?
                WHERE id = ? AND deleted_at IS NULL
                """;
        jdbcTemplate.update(sql, Timestamp.from(now), userId);
    }

    @Override
    public void linkOidcSubjectIfEmpty(UUID userId, String subject, Instant now) {
        if (subject == null || subject.isBlank()) {
            return;
        }
        String sql = """
                UPDATE users
                SET oidc_subject = COALESCE(oidc_subject, ?), updated_at = ?
                WHERE id = ? AND deleted_at IS NULL
                """;
        jdbcTemplate.update(sql, subject, Timestamp.from(now), userId);
    }

    @Override
    public void saveOidcAuthState(String state, String codeVerifier, String redirectUri, Instant expiresAt) {
        String sql = """
                INSERT INTO oidc_auth_states (state, code_verifier, redirect_uri, expires_at)
                VALUES (?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, state, codeVerifier, redirectUri, Timestamp.from(expiresAt));
    }

    @Override
    public Optional<OidcAuthStateRecord> consumeOidcAuthState(String state, Instant now) {
        String selectSql = """
                SELECT state, code_verifier, redirect_uri, expires_at
                FROM oidc_auth_states
                WHERE state = ?
                """;
        Optional<OidcAuthStateRecord> found = jdbcTemplate.query(selectSql, (rs, rowNum) -> new OidcAuthStateRecord(
                rs.getString("state"),
                rs.getString("code_verifier"),
                rs.getString("redirect_uri"),
                rs.getTimestamp("expires_at").toInstant()
        ), state).stream().findFirst();
        jdbcTemplate.update("DELETE FROM oidc_auth_states WHERE state = ? OR expires_at < ?", state, Timestamp.from(now));
        return found.filter(rec -> rec.expiresAt().isAfter(now));
    }

    @Override
    public void clearLoginAttempts(String email, String ipAddress) {
        jdbcTemplate.update(
                "DELETE FROM login_attempts WHERE lower(email) = lower(?) AND ip_address = ?",
                email,
                ipAddress
        );
    }

    @Override
    public void markLoginTimestamps(UUID userId, Instant now) {
        Timestamp ts = Timestamp.from(now);
        jdbcTemplate.update(
                "UPDATE users SET last_login_at = ?, last_activity_at = ?, updated_at = ? WHERE id = ?",
                ts,
                ts,
                ts,
                userId
        );
    }

    @Override
    public void acceptTermsAndActivate(UUID userId, Instant now) {
        Timestamp ts = Timestamp.from(now);
        jdbcTemplate.update(
                """
                UPDATE users
                SET terms_accepted_at = COALESCE(terms_accepted_at, ?),
                    status = CASE WHEN status = 'PENDING' THEN 'ACTIVE' ELSE status END,
                    updated_at = ?
                WHERE id = ? AND deleted_at IS NULL
                """,
                ts,
                ts,
                userId
        );
    }

    @Override
    public void saveRefreshToken(
            UUID userId,
            String tokenHash,
            boolean rememberMe,
            Instant expiresAt,
            Instant now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO refresh_tokens (user_id, token_hash, remember_me, expires_at, created_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                userId,
                tokenHash,
                rememberMe,
                Timestamp.from(expiresAt),
                Timestamp.from(now)
        );
    }

    @Override
    public Optional<RefreshTokenRecord> findRefreshToken(String tokenHash) {
        String sql = """
                SELECT id, user_id, remember_me, expires_at, revoked_at
                FROM refresh_tokens
                WHERE token_hash = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp revoked = rs.getTimestamp("revoked_at");
            return new RefreshTokenRecord(
                    rs.getObject("id", UUID.class),
                    rs.getObject("user_id", UUID.class),
                    rs.getBoolean("remember_me"),
                    rs.getTimestamp("expires_at").toInstant(),
                    revoked != null ? revoked.toInstant() : null
            );
        }, tokenHash).stream().findFirst();
    }

    @Override
    public void revokeRefreshToken(String tokenHash, Instant now) {
        jdbcTemplate.update(
                "UPDATE refresh_tokens SET revoked_at = ? WHERE token_hash = ? AND revoked_at IS NULL",
                Timestamp.from(now),
                tokenHash
        );
    }


    private AuthUserRecord mapUser(ResultSet rs) throws SQLException {
        Timestamp termsAccepted = rs.getTimestamp("terms_accepted_at");
        return new AuthUserRecord(
                rs.getObject("id", UUID.class),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("full_name"),
                SiteRole.valueOf(rs.getString("site_role")),
                rs.getString("status"),
                rs.getBoolean("must_change_password"),
                termsAccepted != null ? termsAccepted.toInstant() : null
        );
    }
}
