package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.GroupDetailDto;
import vn.edu.davs.backend.dto.GroupDto;
import vn.edu.davs.backend.enu.SiteRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC repository for UC04 workgroup management ({@code groups}, {@code group_members}).
 */
@Repository
@RequiredArgsConstructor
public class JdbcGroupRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<GroupDto> listGroups(String q) {
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase() + "%";
            String sql = """
                    SELECT g.id, g.name, g.description, g.min_role, g.is_system, g.created_at, g.updated_at,
                           (SELECT COUNT(*) FROM group_members gm
                            JOIN users u ON u.id = gm.user_id AND u.deleted_at IS NULL
                            WHERE gm.group_id = g.id) AS member_count
                    FROM groups g
                    WHERE lower(g.name) LIKE ? OR lower(COALESCE(g.description, '')) LIKE ?
                    ORDER BY g.is_system DESC, lower(g.name) ASC
                    """;
            return jdbcTemplate.query(sql, (rs, rowNum) -> mapGroupDto(rs), pattern, pattern);
        }
        String sql = """
                SELECT g.id, g.name, g.description, g.min_role, g.is_system, g.created_at, g.updated_at,
                       (SELECT COUNT(*) FROM group_members gm
                        JOIN users u ON u.id = gm.user_id AND u.deleted_at IS NULL
                        WHERE gm.group_id = g.id) AS member_count
                FROM groups g
                ORDER BY g.is_system DESC, lower(g.name) ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapGroupDto(rs));
    }

    public Optional<GroupDto> findById(UUID groupId) {
        if (groupId == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT g.id, g.name, g.description, g.min_role, g.is_system, g.created_at, g.updated_at,
                       (SELECT COUNT(*) FROM group_members gm
                        JOIN users u ON u.id = gm.user_id AND u.deleted_at IS NULL
                        WHERE gm.group_id = g.id) AS member_count
                FROM groups g
                WHERE g.id = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapGroupDto(rs), groupId).stream().findFirst();
    }

    public List<GroupDetailDto.GroupMemberItem> listGroupMembers(UUID groupId) {
        String sql = """
                SELECT u.id, u.email, COALESCE(NULLIF(u.full_name, ''), split_part(u.email, '@', 1)) AS full_name,
                       u.site_role, u.status, gm.created_at
                FROM group_members gm
                JOIN users u ON u.id = gm.user_id
                WHERE gm.group_id = ? AND u.deleted_at IS NULL
                ORDER BY lower(u.email) ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp joined = rs.getTimestamp("created_at");
            return new GroupDetailDto.GroupMemberItem(
                    rs.getObject("id", UUID.class),
                    rs.getString("email"),
                    rs.getString("full_name"),
                    SiteRole.valueOf(rs.getString("site_role")),
                    rs.getString("status"),
                    joined != null ? joined.toInstant() : null
            );
        }, groupId);
    }

    public boolean existsNameCaseInsensitive(String name, UUID excludeId) {
        if (excludeId != null) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM groups WHERE lower(name) = lower(?) AND id <> ?",
                    Integer.class,
                    name,
                    excludeId
            );
            return count != null && count > 0;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM groups WHERE lower(name) = lower(?)",
                Integer.class,
                name
        );
        return count != null && count > 0;
    }

    public UUID insertGroup(String name, String description, SiteRole minRole, UUID createdBy) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO groups (name, description, min_role, is_system, created_by)
                VALUES (?, ?, ?, FALSE, ?)
                RETURNING id
                """,
                UUID.class,
                name,
                description,
                minRole != null ? minRole.name() : null,
                createdBy
        );
    }

    public void updateGroup(UUID groupId, String name, String description, SiteRole minRole) {
        jdbcTemplate.update(
                """
                UPDATE groups
                SET name = ?, description = ?, min_role = ?, updated_at = now()
                WHERE id = ?
                """,
                name,
                description,
                minRole != null ? minRole.name() : null,
                groupId
        );
    }

    public boolean isMember(UUID groupId, UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM group_members WHERE group_id = ? AND user_id = ?",
                Integer.class,
                groupId,
                userId
        );
        return count != null && count > 0;
    }

    public void addMember(UUID groupId, UUID userId) {
        jdbcTemplate.update(
                """
                INSERT INTO group_members (group_id, user_id)
                VALUES (?, ?)
                ON CONFLICT (group_id, user_id) DO NOTHING
                """,
                groupId,
                userId
        );
    }

    public void removeMember(UUID groupId, UUID userId) {
        jdbcTemplate.update(
                "DELETE FROM group_members WHERE group_id = ? AND user_id = ?",
                groupId,
                userId
        );
    }

    public void deleteGroupAndRevokePermissions(UUID groupId) {
        jdbcTemplate.update(
                "DELETE FROM permission_rules WHERE subject_type = 'GROUP' AND subject_id = ?",
                groupId
        );
        jdbcTemplate.update(
                "DELETE FROM groups WHERE id = ?",
                groupId
        );
    }

    private static GroupDto mapGroupDto(ResultSet rs) throws SQLException {
        String minRoleRaw = rs.getString("min_role");
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        return new GroupDto(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("description"),
                minRoleRaw != null && !minRoleRaw.isBlank() ? SiteRole.valueOf(minRoleRaw) : null,
                rs.getBoolean("is_system"),
                rs.getInt("member_count"),
                created != null ? created.toInstant() : null,
                updated != null ? updated.toInstant() : null
        );
    }
}
