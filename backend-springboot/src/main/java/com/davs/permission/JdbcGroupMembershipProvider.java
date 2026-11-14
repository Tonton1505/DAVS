package com.davs.permission;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * JDBC-backed {@link GroupMembershipProvider} reading from {@code group_members}.
 * Returns an empty set gracefully before the UC04 group migration is applied.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcGroupMembershipProvider implements GroupMembershipProvider {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Set<UUID> findGroupIdsForUser(UUID userId) {
        if (userId == null) {
            return Set.of();
        }
        try {
            return new HashSet<>(jdbcTemplate.query(
                    "SELECT group_id FROM group_members WHERE user_id = ?",
                    (rs, rowNum) -> rs.getObject("group_id", UUID.class),
                    userId
            ));
        } catch (DataAccessException ex) {
            log.debug("Unable to query group_members for user {}: {}", userId, ex.getMessage());
            return Set.of();
        }
    }
}
