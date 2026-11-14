package com.davs.project;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC persistence for UC05 project creation, hierarchy depth checks, and rule inheritance.
 */
@Repository
@RequiredArgsConstructor
public class JdbcProjectRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<ProjectDto> listActiveProjects() {
        String sql = """
                SELECT id, name, description, parent_id, owner_id, permission_mode,
                       is_default, created_at, updated_at
                FROM projects
                WHERE deleted_at IS NULL
                ORDER BY is_default DESC, lower(name) ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs, 1));
    }

    public Optional<ProjectDto> findById(UUID id) {
        String sql = """
                SELECT id, name, description, parent_id, owner_id, permission_mode,
                       is_default, created_at, updated_at
                FROM projects
                WHERE id = ? AND deleted_at IS NULL
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs, computeDepth(id)), id).stream().findFirst();
    }

    public boolean existsSiblingName(UUID parentId, String name) {
        if (parentId == null) {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM projects
                    WHERE parent_id IS NULL AND deleted_at IS NULL AND lower(name) = lower(?)
                    """,
                    Integer.class,
                    name
            );
            return count != null && count > 0;
        }
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM projects
                WHERE parent_id = ? AND deleted_at IS NULL AND lower(name) = lower(?)
                """,
                Integer.class,
                parentId,
                name
        );
        return count != null && count > 0;
    }

    public int computeDepth(UUID projectId) {
        int depth = 0;
        UUID current = projectId;
        Set<UUID> visited = new HashSet<>();
        while (current != null && visited.add(current)) {
            depth++;
            List<UUID> parents = jdbcTemplate.query(
                    "SELECT parent_id FROM projects WHERE id = ? AND deleted_at IS NULL",
                    (rs, rowNum) -> rs.getObject("parent_id", UUID.class),
                    current
            );
            if (parents.isEmpty()) {
                return 0;
            }
            current = parents.get(0);
        }
        return depth;
    }

    public UUID insertProject(
            String name,
            String description,
            UUID parentId,
            UUID ownerId,
            String permissionMode
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO projects (name, description, parent_id, owner_id, permission_mode, is_default)
                VALUES (?, ?, ?, ?, ?, FALSE)
                RETURNING id
                """,
                UUID.class,
                name,
                description,
                parentId,
                ownerId,
                permissionMode
        );
    }

    public void copyParentPermissionRules(UUID parentId, UUID childId) {
        jdbcTemplate.update(
                """
                INSERT INTO permission_rules (
                    resource_type, resource_id, subject_type, subject_id, capability, effect, created_by
                )
                SELECT 'PROJECT', ?, subject_type, subject_id, capability, effect, created_by
                FROM permission_rules
                WHERE resource_type = 'PROJECT' AND resource_id = ?
                ON CONFLICT DO NOTHING
                """,
                childId,
                parentId
        );
    }

    private ProjectDto mapRow(ResultSet rs, int depth) throws SQLException {
        return new ProjectDto(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("description"),
                rs.getObject("parent_id", UUID.class),
                rs.getObject("owner_id", UUID.class),
                rs.getString("permission_mode"),
                rs.getBoolean("is_default"),
                depth,
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }
}
