package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.PageResponse;
import vn.edu.davs.backend.dto.UserManagementDto;
import vn.edu.davs.backend.enu.SiteRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC repository for UC03 user listing, role updates, activation/deactivation,
 * invitation resend, and content ownership transfer on deletion.
 */
@Repository
@RequiredArgsConstructor
public class JdbcUserManagementRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public PageResponse<UserManagementDto> searchUsers(
            String q,
            String role,
            String group,
            String status,
            int page,
            int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(200, Math.max(1, size));

        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(" WHERE u.deleted_at IS NULL ");

        if (q != null && !q.isBlank()) {
            params.addValue("q", "%" + q.trim().toLowerCase() + "%");
            where.append(" AND (lower(u.email) LIKE :q OR lower(COALESCE(u.full_name, '')) LIKE :q) ");
        }
        if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role.trim())) {
            params.addValue("role", role.trim().toUpperCase());
            where.append(" AND u.site_role = :role ");
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            params.addValue("status", status.trim().toUpperCase());
            where.append(" AND u.status = :status ");
        }
        if (group != null && !group.isBlank() && !"ALL".equalsIgnoreCase(group.trim())) {
            params.addValue("groupFilter", group.trim().toLowerCase());
            where.append("""
                     AND EXISTS (
                         SELECT 1
                         FROM group_members gm
                         JOIN groups g ON g.id = gm.group_id
                         WHERE gm.user_id = u.id
                           AND (lower(g.name) = :groupFilter OR g.id::text = :groupFilter)
                     )
                    """);
        }

        String countSql = "SELECT COUNT(*) FROM users u" + where;
        Long total = namedJdbcTemplate.queryForObject(countSql, params, Long.class);
        long totalElements = total != null ? total : 0L;

        params.addValue("limit", safeSize);
        params.addValue("offset", (long) safePage * safeSize);

        String listSql = """
                SELECT u.id, u.email, COALESCE(NULLIF(u.full_name, ''), split_part(u.email, '@', 1)) AS full_name,
                       u.site_role, u.auth_method, u.status, u.last_login_at, u.created_at
                FROM users u
                """ + where + """
                ORDER BY u.created_at DESC, lower(u.email) ASC
                LIMIT :limit OFFSET :offset
                """;

        List<UserRow> rows = namedJdbcTemplate.query(listSql, params, (rs, rowNum) -> mapUserRow(rs));
        if (rows.isEmpty()) {
            return new PageResponse<>(List.of(), safePage, safeSize, totalElements);
        }

        List<UUID> userIds = rows.stream().map(UserRow::id).toList();
        Map<UUID, List<String>> groupsByUser = loadGroupsForUsers(userIds);
        Map<UUID, Integer> ownedCounts = loadOwnedCountsForUsers(userIds);

        List<UserManagementDto> dtos = rows.stream()
                .map(r -> new UserManagementDto(
                        r.id(),
                        r.email(),
                        r.fullName(),
                        r.siteRole(),
                        r.authMethod(),
                        r.status(),
                        groupsByUser.getOrDefault(r.id(), List.of()),
                        r.lastLoginAt(),
                        ownedCounts.getOrDefault(r.id(), 0),
                        r.createdAt()
                ))
                .toList();

        return new PageResponse<>(dtos, safePage, safeSize, totalElements);
    }

    public Optional<UserManagementDto> findUserById(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT u.id, u.email, COALESCE(NULLIF(u.full_name, ''), split_part(u.email, '@', 1)) AS full_name,
                       u.site_role, u.auth_method, u.status, u.last_login_at, u.created_at
                FROM users u
                WHERE u.id = ? AND u.deleted_at IS NULL
                """;
        List<UserRow> rows = jdbcTemplate.query(sql, (rs, rowNum) -> mapUserRow(rs), userId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        UserRow r = rows.get(0);
        Map<UUID, List<String>> groups = loadGroupsForUsers(List.of(userId));
        int owned = countOwnedResources(userId);
        return Optional.of(new UserManagementDto(
                r.id(),
                r.email(),
                r.fullName(),
                r.siteRole(),
                r.authMethod(),
                r.status(),
                groups.getOrDefault(userId, List.of()),
                r.lastLoginAt(),
                owned,
                r.createdAt()
        ));
    }

    public int countActiveSiteAdmins() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM users
                WHERE site_role = 'SITE_ADMIN'
                  AND status = 'ACTIVE'
                  AND deleted_at IS NULL
                """,
                Integer.class
        );
        return count != null ? count : 0;
    }

    public int countOwnedResources(UUID userId) {
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM projects WHERE owner_id = ? AND deleted_at IS NULL)
                  + (SELECT COUNT(*) FROM workbooks WHERE owner_id = ? AND deleted_at IS NULL)
                  + (SELECT COUNT(*) FROM data_sources WHERE owner_id = ? AND deleted_at IS NULL)
                  + (SELECT COUNT(*) FROM prep_flows WHERE owner_id = ? AND deleted_at IS NULL)
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, userId, userId, userId);
        return count != null ? count : 0;
    }

    public boolean reserveLicense(SiteRole role, int count) {
        if (count <= 0) {
            return true;
        }
        int updated = jdbcTemplate.update(
                """
                UPDATE license_pools
                SET used = used + ?, updated_at = now()
                WHERE role = ? AND used + ? <= total
                """,
                count,
                role.name(),
                count
        );
        return updated == 1;
    }

    public void releaseLicense(SiteRole role, int count) {
        if (count <= 0) {
            return;
        }
        jdbcTemplate.update(
                """
                UPDATE license_pools
                SET used = GREATEST(0, used - ?), updated_at = now()
                WHERE role = ?
                """,
                count,
                role.name()
        );
    }

    public void updateUserRole(UUID userId, SiteRole newRole) {
        jdbcTemplate.update(
                "UPDATE users SET site_role = ?, updated_at = now() WHERE id = ? AND deleted_at IS NULL",
                newRole.name(),
                userId
        );
    }

    public void updateUserStatus(UUID userId, String status) {
        jdbcTemplate.update(
                "UPDATE users SET status = ?, updated_at = now() WHERE id = ? AND deleted_at IS NULL",
                status,
                userId
        );
    }

    public void revokeAllRefreshTokens(UUID userId) {
        jdbcTemplate.update(
                "UPDATE refresh_tokens SET revoked_at = now() WHERE user_id = ? AND revoked_at IS NULL",
                userId
        );
    }

    public void transferOwnership(UUID fromUserId, UUID toUserId) {
        jdbcTemplate.update(
                "UPDATE projects SET owner_id = ?, updated_at = now() WHERE owner_id = ? AND deleted_at IS NULL",
                toUserId,
                fromUserId
        );
        jdbcTemplate.update(
                "UPDATE workbooks SET owner_id = ?, updated_at = now() WHERE owner_id = ? AND deleted_at IS NULL",
                toUserId,
                fromUserId
        );
        jdbcTemplate.update(
                "UPDATE data_sources SET owner_id = ?, updated_at = now() WHERE owner_id = ? AND deleted_at IS NULL",
                toUserId,
                fromUserId
        );
        jdbcTemplate.update(
                "UPDATE prep_flows SET owner_id = ?, updated_at = now() WHERE owner_id = ? AND deleted_at IS NULL",
                toUserId,
                fromUserId
        );
    }

    public void softDeleteUser(UUID userId) {
        jdbcTemplate.update(
                "DELETE FROM group_members WHERE user_id = ?",
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM permission_rules WHERE subject_type = 'USER' AND subject_id = ?",
                userId
        );
        revokeAllRefreshTokens(userId);
        jdbcTemplate.update(
                "UPDATE users SET status = 'DISABLED', deleted_at = now(), updated_at = now() WHERE id = ?",
                userId
        );
    }

    private Map<UUID, List<String>> loadGroupsForUsers(List<UUID> userIds) {
        Map<UUID, List<String>> result = new LinkedHashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }
        String sql = """
                SELECT gm.user_id, g.name
                FROM group_members gm
                JOIN groups g ON g.id = gm.group_id
                WHERE gm.user_id IN (:userIds)
                ORDER BY lower(g.name) ASC
                """;
        namedJdbcTemplate.query(
                sql,
                Map.of("userIds", userIds),
                rs -> {
                    UUID uid = rs.getObject("user_id", UUID.class);
                    String gName = rs.getString("name");
                    result.computeIfAbsent(uid, ignored -> new ArrayList<>()).add(gName);
                }
        );
        return result;
    }

    private Map<UUID, Integer> loadOwnedCountsForUsers(List<UUID> userIds) {
        Map<UUID, Integer> result = new LinkedHashMap<>();
        for (UUID uid : userIds) {
            result.put(uid, countOwnedResources(uid));
        }
        return result;
    }

    private static UserRow mapUserRow(ResultSet rs) throws SQLException {
        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        Timestamp created = rs.getTimestamp("created_at");
        return new UserRow(
                rs.getObject("id", UUID.class),
                rs.getString("email"),
                rs.getString("full_name"),
                SiteRole.valueOf(rs.getString("site_role")),
                rs.getString("auth_method"),
                rs.getString("status"),
                lastLogin != null ? lastLogin.toInstant() : null,
                created != null ? created.toInstant() : null
        );
    }

    private record UserRow(
            UUID id,
            String email,
            String fullName,
            SiteRole siteRole,
            String authMethod,
            String status,
            Instant lastLoginAt,
            Instant createdAt
    ) {
    }
}
