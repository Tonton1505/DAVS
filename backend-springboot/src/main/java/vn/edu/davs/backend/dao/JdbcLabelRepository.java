package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.LabelCategoryDto;
import vn.edu.davs.backend.dto.LabelDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC persistence for UC09 Data Labels, Categories, and Target Assignments.
 */
@Repository
@RequiredArgsConstructor
public class JdbcLabelRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<LabelCategoryDto> listCategories() {
        String sql = """
                SELECT id, name, description, is_warning, created_by, created_at
                FROM label_categories
                ORDER BY is_warning DESC, lower(name) ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapCategory(rs));
    }

    public Optional<LabelCategoryDto> findCategoryById(UUID id) {
        String sql = """
                SELECT id, name, description, is_warning, created_by, created_at
                FROM label_categories
                WHERE id = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapCategory(rs), id).stream().findFirst();
    }

    public boolean existsCategoryByName(String name) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM label_categories WHERE lower(name) = lower(?)",
                Integer.class,
                name
        );
        return count != null && count > 0;
    }

    public UUID insertCategory(String name, String description, boolean isWarning, UUID createdBy) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO label_categories (name, description, is_warning, created_by)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """,
                UUID.class,
                name,
                description,
                isWarning,
                createdBy
        );
    }

    public List<LabelDto> listLabels(UUID categoryId, String query) {
        StringBuilder sql = new StringBuilder("""
                SELECT l.id, l.category_id, c.name AS category_name, l.value, l.description,
                       l.is_warning, l.created_by, l.created_at, l.updated_at,
                       (SELECT COUNT(*) FROM label_assignments a WHERE a.label_id = l.id) AS assigned_count
                FROM labels l
                INNER JOIN label_categories c ON c.id = l.category_id
                WHERE 1 = 1
                """);
        List<Object> params = new ArrayList<>();
        if (categoryId != null) {
            sql.append(" AND l.category_id = ?");
            params.add(categoryId);
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (lower(l.value) LIKE lower(?) OR lower(COALESCE(l.description, '')) LIKE lower(?))");
            String pattern = "%" + query.trim() + "%";
            params.add(pattern);
            params.add(pattern);
        }
        sql.append(" ORDER BY l.is_warning DESC, lower(c.name) ASC, lower(l.value) ASC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapLabel(rs), params.toArray());
    }

    public Optional<LabelDto> findLabelById(UUID id) {
        String sql = """
                SELECT l.id, l.category_id, c.name AS category_name, l.value, l.description,
                       l.is_warning, l.created_by, l.created_at, l.updated_at,
                       (SELECT COUNT(*) FROM label_assignments a WHERE a.label_id = l.id) AS assigned_count
                FROM labels l
                INNER JOIN label_categories c ON c.id = l.category_id
                WHERE l.id = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapLabel(rs), id).stream().findFirst();
    }

    public boolean existsLabelInCategory(UUID categoryId, String value, UUID excludeLabelId) {
        if (excludeLabelId == null) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM labels WHERE category_id = ? AND lower(value) = lower(?)",
                    Integer.class,
                    categoryId,
                    value
            );
            return count != null && count > 0;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM labels WHERE category_id = ? AND lower(value) = lower(?) AND id <> ?",
                Integer.class,
                categoryId,
                value,
                excludeLabelId
        );
        return count != null && count > 0;
    }

    public UUID insertLabel(
            UUID categoryId,
            String value,
            String description,
            boolean isWarning,
            UUID createdBy
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO labels (category_id, value, description, is_warning, created_by)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """,
                UUID.class,
                categoryId,
                value,
                description,
                isWarning,
                createdBy
        );
    }

    public void updateLabel(UUID id, String value, String description, boolean isWarning) {
        jdbcTemplate.update(
                """
                UPDATE labels
                SET value = ?, description = ?, is_warning = ?, updated_at = now()
                WHERE id = ?
                """,
                value,
                description,
                isWarning,
                id
        );
    }

    public int countAssignments(UUID labelId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM label_assignments WHERE label_id = ?",
                Integer.class,
                labelId
        );
        return count != null ? count : 0;
    }

    public void deleteLabel(UUID id) {
        jdbcTemplate.update("DELETE FROM labels WHERE id = ?", id);
    }

    public int assignLabel(UUID labelId, String targetType, UUID targetId, UUID assignedBy) {
        return jdbcTemplate.update(
                """
                INSERT INTO label_assignments (label_id, target_type, target_id, assigned_by)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (label_id, target_type, target_id) DO NOTHING
                """,
                labelId,
                targetType,
                targetId,
                assignedBy
        );
    }

    public int unassignLabel(UUID labelId, String targetType, UUID targetId) {
        return jdbcTemplate.update(
                """
                DELETE FROM label_assignments
                WHERE label_id = ? AND target_type = ? AND target_id = ?
                """,
                labelId,
                targetType,
                targetId
        );
    }

    public List<LabelDto> listLabelsForTarget(String targetType, UUID targetId) {
        String sql = """
                SELECT l.id, l.category_id, c.name AS category_name, l.value, l.description,
                       l.is_warning, l.created_by, l.created_at, l.updated_at,
                       (SELECT COUNT(*) FROM label_assignments a2 WHERE a2.label_id = l.id) AS assigned_count
                FROM label_assignments a
                INNER JOIN labels l ON l.id = a.label_id
                INNER JOIN label_categories c ON c.id = l.category_id
                WHERE a.target_type = ? AND a.target_id = ?
                ORDER BY l.is_warning DESC, lower(c.name) ASC, lower(l.value) ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapLabel(rs), targetType, targetId);
    }

    private LabelCategoryDto mapCategory(ResultSet rs) throws SQLException {
        return new LabelCategoryDto(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("is_warning"),
                rs.getObject("created_by", UUID.class),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private LabelDto mapLabel(ResultSet rs) throws SQLException {
        return new LabelDto(
                rs.getObject("id", UUID.class),
                rs.getObject("category_id", UUID.class),
                rs.getString("category_name"),
                rs.getString("value"),
                rs.getString("description"),
                rs.getBoolean("is_warning"),
                rs.getInt("assigned_count"),
                rs.getObject("created_by", UUID.class),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }
}
