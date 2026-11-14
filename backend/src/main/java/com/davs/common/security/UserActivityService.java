package com.davs.common.security;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Reads and updates {@code users.last_activity_at} and account status for session idle-timeout enforcement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final JdbcTemplate jdbcTemplate;

    public Optional<UserSessionSnapshot> findSessionSnapshot(UUID userId) {
        try {
            List<UserSessionSnapshot> rows = jdbcTemplate.query(
                    "SELECT last_activity_at, status FROM users WHERE id = ? AND deleted_at IS NULL",
                    (rs, rowNum) -> {
                        Timestamp ts = rs.getTimestamp("last_activity_at");
                        return new UserSessionSnapshot(
                                ts != null ? ts.toInstant() : null,
                                rs.getString("status")
                        );
                    },
                    userId
            );
            return rows.stream().findFirst();
        } catch (DataAccessException ex) {
            log.warn("Unable to query last_activity_at for user {}: {}", userId, ex.getMessage());
            return Optional.empty();
        }
    }

    public void touchLastActivity(UUID userId, Instant activityTime) {
        try {
            jdbcTemplate.update(
                    "UPDATE users SET last_activity_at = ?, updated_at = ? WHERE id = ? AND deleted_at IS NULL",
                    Timestamp.from(activityTime),
                    Timestamp.from(activityTime),
                    userId
            );
        } catch (DataAccessException ex) {
            log.warn("Unable to update last_activity_at for user {}: {}", userId, ex.getMessage());
        }
    }

    public record UserSessionSnapshot(Instant lastActivityAt, String status) {
    }
}
