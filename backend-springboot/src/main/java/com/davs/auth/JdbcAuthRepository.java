package com.davs.auth;

import com.davs.common.security.SiteRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC persistence operations for UC01 authentication, lockout tracking, and refresh tokens.
 */
@Repository
@RequiredArgsConstructor
public class JdbcAuthRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<AuthUserRecord> findActiveUserByEmail(String email) {
        String sql = """
                SELECT id, email, password_hash, full_name, site_role, status,
                       must_change_password, terms_accepted_at
                FROM users
                WHERE lower(email) = lower(?) AND deleted_at IS NULL
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapUser(rs), email).stream().findFirst();
    }

    public Optional<AuthUserRecord> findActiveUserById(UUID userId) {
        String sql = """
                SELECT id, email, password_hash, full_name, site_role, status,
                       must_change_password, terms_accepted_at
                FROM users
                WHERE id = ? AND deleted_at IS NULL
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapUser(rs), userId).stream().findFirst();
    }

    public boolean hasValidLicensePool(SiteRole siteRole) {
        String sql = """
                SELECT COUNT(*)
                FROM license_pools
                WHERE role = ? AND total > 0
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, siteRole.name());
        return count != null && count > 0;
    }

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

    public void clearLoginAttempts(String email, String ipAddress) {
        jdbcTemplate.update(
                "DELETE FROM login_attempts WHERE lower(email) = lower(?) AND ip_address = ?",
                email,
                ipAddress
        );
    }

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
