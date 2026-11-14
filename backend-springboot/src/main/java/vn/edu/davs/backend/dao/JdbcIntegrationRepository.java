package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.IntegrationDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC persistence for UC25 external platform integrations ({@code integrations}).
 */
@Repository
@RequiredArgsConstructor
public class JdbcIntegrationRepository {

    private final JdbcTemplate jdbcTemplate;

    public void markExpiredTokensReconnectRequired() {
        jdbcTemplate.update(
                """
                UPDATE integrations
                SET status = 'RECONNECT_REQUIRED', updated_at = now()
                WHERE status = 'CONNECTED'
                  AND token_expires_at IS NOT NULL
                  AND token_expires_at < now()
                """
        );
    }

    public List<IntegrationDto> listAll() {
        String sql = """
                SELECT platform, display_name, description, status, scopes, account_name,
                       (encrypted_access_token IS NOT NULL AND length(encrypted_access_token) > 0) AS has_token,
                       token_expires_at, connected_at, connected_by, updated_at
                FROM integrations
                ORDER BY CASE platform WHEN 'SLACK' THEN 1 WHEN 'SALESFORCE' THEN 2 ELSE 3 END
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapDto(rs));
    }

    public Optional<IntegrationDto> findByPlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return Optional.empty();
        }
        String sql = """
                SELECT platform, display_name, description, status, scopes, account_name,
                       (encrypted_access_token IS NOT NULL AND length(encrypted_access_token) > 0) AS has_token,
                       token_expires_at, connected_at, connected_by, updated_at
                FROM integrations
                WHERE upper(platform) = upper(?)
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapDto(rs), platform.trim()).stream().findFirst();
    }

    public Optional<String> findOauthState(String platform) {
        return jdbcTemplate.query(
                "SELECT oauth_state FROM integrations WHERE upper(platform) = upper(?)",
                (rs, rowNum) -> rs.getString("oauth_state"),
                platform.trim()
        ).stream().findFirst();
    }

    public Optional<String> findEncryptedAccessToken(String platform) {
        return jdbcTemplate.query(
                "SELECT encrypted_access_token FROM integrations WHERE upper(platform) = upper(?)",
                (rs, rowNum) -> rs.getString("encrypted_access_token"),
                platform.trim()
        ).stream().findFirst();
    }

    public void saveOauthState(String platform, String state) {
        jdbcTemplate.update(
                "UPDATE integrations SET oauth_state = ?, updated_at = now() WHERE upper(platform) = upper(?)",
                state,
                platform.trim()
        );
    }

    public void markConnected(
            String platform,
            String encryptedAccessToken,
            String encryptedRefreshToken,
            String accountName,
            Instant tokenExpiresAt,
            UUID connectedBy
    ) {
        jdbcTemplate.update(
                """
                UPDATE integrations
                SET status = 'CONNECTED',
                    encrypted_access_token = ?,
                    encrypted_refresh_token = ?,
                    oauth_state = NULL,
                    account_name = ?,
                    token_expires_at = ?,
                    connected_at = now(),
                    connected_by = ?,
                    updated_at = now()
                WHERE upper(platform) = upper(?)
                """,
                encryptedAccessToken,
                encryptedRefreshToken,
                accountName,
                tokenExpiresAt != null ? Timestamp.from(tokenExpiresAt) : null,
                connectedBy,
                platform.trim()
        );
    }

    public void markDisconnected(String platform) {
        jdbcTemplate.update(
                """
                UPDATE integrations
                SET status = 'DISCONNECTED',
                    encrypted_access_token = NULL,
                    encrypted_refresh_token = NULL,
                    oauth_state = NULL,
                    account_name = NULL,
                    token_expires_at = NULL,
                    updated_at = now()
                WHERE upper(platform) = upper(?)
                """,
                platform.trim()
        );
    }

    private static IntegrationDto mapDto(ResultSet rs) throws SQLException {
        Timestamp expires = rs.getTimestamp("token_expires_at");
        Timestamp connected = rs.getTimestamp("connected_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        return new IntegrationDto(
                rs.getString("platform"),
                rs.getString("display_name"),
                rs.getString("description"),
                rs.getString("status"),
                rs.getString("scopes"),
                rs.getString("account_name"),
                rs.getBoolean("has_token"),
                expires != null ? expires.toInstant() : null,
                connected != null ? connected.toInstant() : null,
                rs.getObject("connected_by", UUID.class),
                updated != null ? updated.toInstant() : null
        );
    }
}
