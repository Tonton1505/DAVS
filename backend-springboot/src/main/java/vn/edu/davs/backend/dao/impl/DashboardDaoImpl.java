package vn.edu.davs.backend.dao.impl;

import vn.edu.davs.backend.dto.DashboardDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.edu.davs.backend.dao.IDashboardDao;

@RequiredArgsConstructor
@Repository
public class DashboardDaoImpl implements IDashboardDao {


    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<DashboardDto> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, workbook_id, name, description, definition, created_by, created_at, updated_at
                FROM dashboards
                WHERE id = ? AND deleted_at IS NULL
                """;
        List<DashboardDto> list = jdbcTemplate.query(sql, this::mapRow, id);
        return list.stream().findFirst();
    }

    @Override
    public List<DashboardDto> findByWorkbookId(UUID workbookId) {
        String sql = """
                SELECT id, workbook_id, name, description, definition, created_by, created_at, updated_at
                FROM dashboards
                WHERE workbook_id = ? AND deleted_at IS NULL
                ORDER BY created_at ASC
                """;
        return jdbcTemplate.query(sql, this::mapRow, workbookId);
    }

    @Override
    public Optional<DashboardDto> findFirstActive() {
        String sql = """
                SELECT id, workbook_id, name, description, definition, created_by, created_at, updated_at
                FROM dashboards
                WHERE deleted_at IS NULL
                ORDER BY created_at ASC
                LIMIT 1
                """;
        List<DashboardDto> list = jdbcTemplate.query(sql, this::mapRow);
        return list.stream().findFirst();
    }

    @Override
    public DashboardDto create(UUID workbookId, String name, String description, String definition, UUID createdBy) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String jsonDef = (definition != null && !definition.isBlank()) ? definition : "{}";
        String sql = """
                INSERT INTO dashboards (id, workbook_id, name, description, definition, created_by, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, id, workbookId, name, description, jsonDef, createdBy, now, now);
        return new DashboardDto(id, workbookId, name, description, jsonDef, createdBy, now, now);
    }

    @Override
    public void update(UUID id, String name, String description, String definition) {
        String sql = """
                UPDATE dashboards
                SET name = COALESCE(?, name),
                    description = COALESCE(?, description),
                    definition = COALESCE(?::jsonb, definition),
                    updated_at = now()
                WHERE id = ? AND deleted_at IS NULL
                """;
        jdbcTemplate.update(sql, name, description, definition, id);
    }

    @Override
    public void delete(UUID id) {
        String sql = "UPDATE dashboards SET deleted_at = now() WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }


    private DashboardDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DashboardDto(
                rs.getObject("id", UUID.class),
                rs.getObject("workbook_id", UUID.class),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("definition"),
                rs.getObject("created_by", UUID.class),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }
}
