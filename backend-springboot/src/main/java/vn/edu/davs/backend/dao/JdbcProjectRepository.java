package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.CollectionDto;
import vn.edu.davs.backend.dto.ProjectDto;
import vn.edu.davs.backend.enu.SiteRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC persistence for UC05 &amp; UC06 project creation, hierarchy management, soft-delete, and collections.
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
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UUID id = rs.getObject("id", UUID.class);
            return mapRow(rs, computeDepth(id));
        });
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

    public Optional<DeletedProjectRow> findDeletedById(UUID id) {
        String sql = """
                SELECT id, name, description, parent_id, owner_id, permission_mode,
                       is_default, created_at, updated_at, deleted_at
                FROM projects
                WHERE id = ? AND deleted_at IS NOT NULL
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp deletedTs = rs.getTimestamp("deleted_at");
            return new DeletedProjectRow(
                    mapRow(rs, 1),
                    deletedTs != null ? deletedTs.toInstant() : null
            );
        }, id).stream().findFirst();
    }

    public boolean existsSiblingName(UUID parentId, String name) {
        return existsSiblingNameExcluding(parentId, name, null);
    }

    public boolean existsSiblingNameExcluding(UUID parentId, String name, UUID excludeId) {
        if (parentId == null) {
            if (excludeId == null) {
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
                    WHERE parent_id IS NULL AND deleted_at IS NULL AND lower(name) = lower(?) AND id <> ?
                    """,
                    Integer.class,
                    name,
                    excludeId
            );
            return count != null && count > 0;
        }
        if (excludeId == null) {
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
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM projects
                WHERE parent_id = ? AND deleted_at IS NULL AND lower(name) = lower(?) AND id <> ?
                """,
                Integer.class,
                parentId,
                name,
                excludeId
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

    public int computeSubtreeHeight(UUID projectId) {
        List<UUID> children = jdbcTemplate.query(
                "SELECT id FROM projects WHERE parent_id = ? AND deleted_at IS NULL",
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                projectId
        );
        int maxChild = 0;
        for (UUID childId : children) {
            maxChild = Math.max(maxChild, computeSubtreeHeight(childId));
        }
        return 1 + maxChild;
    }

    public boolean isDescendantOf(UUID candidateId, UUID ancestorId) {
        if (candidateId == null || ancestorId == null) {
            return false;
        }
        UUID current = candidateId;
        Set<UUID> visited = new HashSet<>();
        while (current != null && visited.add(current)) {
            if (current.equals(ancestorId)) {
                return true;
            }
            List<UUID> parents = jdbcTemplate.query(
                    "SELECT parent_id FROM projects WHERE id = ? AND deleted_at IS NULL",
                    (rs, rowNum) -> rs.getObject("parent_id", UUID.class),
                    current
            );
            if (parents.isEmpty()) {
                return false;
            }
            current = parents.get(0);
        }
        return false;
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

    public void updateProject(UUID id, String name, String description) {
        jdbcTemplate.update(
                """
                UPDATE projects
                SET name = ?, description = ?, updated_at = now()
                WHERE id = ? AND deleted_at IS NULL
                """,
                name,
                description,
                id
        );
    }

    public void moveProject(UUID id, UUID targetParentId) {
        jdbcTemplate.update(
                """
                UPDATE projects
                SET parent_id = ?, updated_at = now()
                WHERE id = ? AND deleted_at IS NULL
                """,
                targetParentId,
                id
        );
    }

    public void updateOwner(UUID id, UUID newOwnerId) {
        jdbcTemplate.update(
                """
                UPDATE projects
                SET owner_id = ?, updated_at = now()
                WHERE id = ? AND deleted_at IS NULL
                """,
                newOwnerId,
                id
        );
    }

    public void softDeleteProject(UUID id) {
        jdbcTemplate.update(
                """
                WITH RECURSIVE subtree AS (
                    SELECT id FROM projects WHERE id = ?
                    UNION ALL
                    SELECT p.id FROM projects p INNER JOIN subtree s ON p.parent_id = s.id
                )
                UPDATE projects
                SET deleted_at = now(), updated_at = now()
                WHERE id IN (SELECT id FROM subtree) AND deleted_at IS NULL
                """,
                id
        );
        jdbcTemplate.update(
                """
                UPDATE workbooks
                SET deleted_at = now(), updated_at = now()
                WHERE project_id = ? AND deleted_at IS NULL
                """,
                id
        );
    }

    public void restoreProject(UUID id) {
        jdbcTemplate.update(
                """
                WITH RECURSIVE subtree AS (
                    SELECT id FROM projects WHERE id = ?
                    UNION ALL
                    SELECT p.id FROM projects p INNER JOIN subtree s ON p.parent_id = s.id
                )
                UPDATE projects
                SET deleted_at = NULL, updated_at = now()
                WHERE id IN (SELECT id FROM subtree)
                """,
                id
        );
        jdbcTemplate.update(
                """
                UPDATE workbooks
                SET deleted_at = NULL, updated_at = now()
                WHERE project_id = ?
                """,
                id
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

    public void replaceProjectRulesFromParent(UUID parentId, UUID childId) {
        jdbcTemplate.update(
                "DELETE FROM permission_rules WHERE resource_type = 'PROJECT' AND resource_id = ?",
                childId
        );
        copyParentPermissionRules(parentId, childId);
    }

    public Optional<OwnerCandidateRow> findUserForOwnership(UUID userId) {
        String sql = """
                SELECT id, email, COALESCE(NULLIF(full_name, ''), email) AS full_name, site_role, status
                FROM users
                WHERE id = ? AND deleted_at IS NULL
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new OwnerCandidateRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        SiteRole.valueOf(rs.getString("site_role")),
                        rs.getString("status")
                ),
                userId
        ).stream().findFirst();
    }

    public String findOwnerDisplayName(UUID ownerId) {
        if (ownerId == null) {
            return "Hệ thống";
        }
        List<String> names = jdbcTemplate.query(
                "SELECT COALESCE(NULLIF(full_name, ''), email) FROM users WHERE id = ?",
                (rs, rowNum) -> rs.getString(1),
                ownerId
        );
        return names.isEmpty() ? "Hệ thống" : names.get(0);
    }

    public int countChildItems(UUID projectId) {
        Integer subProjects = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM projects WHERE parent_id = ? AND deleted_at IS NULL",
                Integer.class,
                projectId
        );
        Integer workbooks = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workbooks WHERE project_id = ? AND deleted_at IS NULL",
                Integer.class,
                projectId
        );
        return (subProjects != null ? subProjects : 0) + (workbooks != null ? workbooks : 0);
    }

    public List<CollectionDto> listCollectionsByOwner(UUID ownerId) {
        String sql = """
                SELECT id, name, description, owner_id, created_at, updated_at
                FROM collections
                WHERE owner_id = ?
                ORDER BY updated_at DESC, lower(name) ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UUID colId = rs.getObject("id", UUID.class);
            List<CollectionDto.CollectionItemDto> items = listCollectionItems(colId);
            return new CollectionDto(
                    colId,
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getObject("owner_id", UUID.class),
                    items.size(),
                    items,
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }, ownerId);
    }

    public Optional<CollectionDto> findCollectionById(UUID collectionId) {
        String sql = """
                SELECT id, name, description, owner_id, created_at, updated_at
                FROM collections
                WHERE id = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UUID colId = rs.getObject("id", UUID.class);
            List<CollectionDto.CollectionItemDto> items = listCollectionItems(colId);
            return new CollectionDto(
                    colId,
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getObject("owner_id", UUID.class),
                    items.size(),
                    items,
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }, collectionId).stream().findFirst();
    }

    public UUID insertCollection(String name, String description, UUID ownerId) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO collections (name, description, owner_id)
                VALUES (?, ?, ?)
                RETURNING id
                """,
                UUID.class,
                name,
                description,
                ownerId
        );
    }

    public void addCollectionItem(UUID collectionId, String resourceType, UUID resourceId) {
        jdbcTemplate.update(
                """
                INSERT INTO collection_items (collection_id, resource_type, resource_id)
                VALUES (?, ?, ?)
                ON CONFLICT (collection_id, resource_type, resource_id) DO NOTHING
                """,
                collectionId,
                resourceType,
                resourceId
        );
        jdbcTemplate.update(
                "UPDATE collections SET updated_at = now() WHERE id = ?",
                collectionId
        );
    }

    public List<CollectionDto.CollectionItemDto> listCollectionItems(UUID collectionId) {
        String sql = """
                SELECT ci.collection_id, ci.resource_type, ci.resource_id, ci.added_at,
                       COALESCE(p.name, w.name, ci.resource_id::text) AS resource_name
                FROM collection_items ci
                LEFT JOIN projects p ON ci.resource_type = 'PROJECT' AND p.id = ci.resource_id
                LEFT JOIN workbooks w ON ci.resource_type = 'WORKBOOK' AND w.id = ci.resource_id
                WHERE ci.collection_id = ?
                ORDER BY ci.added_at DESC
                """;
        return new ArrayList<>(jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new CollectionDto.CollectionItemDto(
                        rs.getObject("collection_id", UUID.class),
                        rs.getString("resource_type"),
                        rs.getObject("resource_id", UUID.class),
                        rs.getString("resource_name"),
                        rs.getTimestamp("added_at").toInstant()
                ),
                collectionId
        ));
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

    public record DeletedProjectRow(ProjectDto project, Instant deletedAt) {
    }

    public record OwnerCandidateRow(
            UUID id,
            String email,
            String fullName,
            SiteRole siteRole,
            String status
    ) {
    }
}
