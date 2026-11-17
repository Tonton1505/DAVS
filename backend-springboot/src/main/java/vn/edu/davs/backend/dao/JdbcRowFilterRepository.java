package vn.edu.davs.backend.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.davs.backend.dto.DatasourceRlsStatusDto;
import vn.edu.davs.backend.dto.RowFilterDto;
import vn.edu.davs.backend.dto.SaveRowFilterRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC persistence for Row-Level Security (RLS) policies and per-datasource toggle (T-101).
 */
@Repository
@RequiredArgsConstructor
public class JdbcRowFilterRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public boolean isRlsEnabled(UUID datasourceId) {
        if (datasourceId == null) {
            return false;
        }
        List<Boolean> results = jdbcTemplate.query(
                "SELECT rls_enabled FROM data_sources WHERE id = ? AND deleted_at IS NULL",
                (rs, rowNum) -> rs.getBoolean("rls_enabled"),
                datasourceId
        );
        return !results.isEmpty() && Boolean.TRUE.equals(results.get(0));
    }

    public boolean setRlsEnabled(UUID datasourceId, boolean enabled) {
        int updated = jdbcTemplate.update(
                "UPDATE data_sources SET rls_enabled = ?, updated_at = now() WHERE id = ? AND deleted_at IS NULL",
                enabled,
                datasourceId
        );
        return updated > 0;
    }

    public List<DatasourceRlsStatusDto> listDatasourceRlsStatuses() {
        String sql = """
                SELECT id, name, rls_enabled
                FROM data_sources
                WHERE deleted_at IS NULL
                ORDER BY lower(name) ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new DatasourceRlsStatusDto(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getBoolean("rls_enabled")
        ));
    }

    public List<RowFilterDto> listRowFilters(UUID datasourceId) {
        String baseSql = """
                SELECT f.id,
                       f.datasource_id,
                       ds.name AS datasource_name,
                       f.subject_type,
                       f.subject_id,
                       CASE
                           WHEN f.subject_type = 'USER'
                               THEN COALESCE(NULLIF(u.full_name, ''), u.email, f.subject_id::text)
                           ELSE COALESCE(g.name, f.subject_id::text)
                       END AS subject_name,
                       f.field_name,
                       f.allowed_values,
                       f.created_at,
                       f.updated_at
                FROM user_row_filters f
                JOIN data_sources ds ON ds.id = f.datasource_id
                LEFT JOIN users u ON f.subject_type = 'USER' AND u.id = f.subject_id
                LEFT JOIN groups g ON f.subject_type = 'GROUP' AND g.id = f.subject_id
                """;
        if (datasourceId != null) {
            return jdbcTemplate.query(
                    baseSql + " WHERE f.datasource_id = ? ORDER BY lower(f.field_name) ASC, f.created_at DESC",
                    this::mapRowFilterDto,
                    datasourceId
            );
        }
        return jdbcTemplate.query(
                baseSql + " ORDER BY lower(ds.name) ASC, lower(f.field_name) ASC, f.created_at DESC",
                this::mapRowFilterDto
        );
    }

    public Map<String, List<String>> findFiltersForUser(UUID datasourceId, UUID userId) {
        if (datasourceId == null || userId == null) {
            return Map.of();
        }
        String sql = """
                SELECT f.field_name, f.allowed_values
                FROM user_row_filters f
                WHERE f.datasource_id = ?
                  AND (
                      (f.subject_type = 'USER' AND f.subject_id = ?)
                      OR (f.subject_type = 'GROUP' AND f.subject_id IN (
                          SELECT gm.group_id FROM group_members gm WHERE gm.user_id = ?
                      ))
                  )
                """;
        Map<String, Set<String>> merged = new LinkedHashMap<>();
        jdbcTemplate.query(sql, rs -> {
            String fieldName = rs.getString("field_name");
            List<String> values = parseValues(rs.getString("allowed_values"));
            merged.computeIfAbsent(fieldName, k -> new LinkedHashSet<>()).addAll(values);
        }, datasourceId, userId, userId);

        Map<String, List<String>> result = new LinkedHashMap<>();
        merged.forEach((k, v) -> result.put(k, List.copyOf(v)));
        return result;
    }

    public RowFilterDto upsertRowFilter(SaveRowFilterRequest request, UUID actorId) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String valuesJson = toJson(request.allowedValues());
        String sql = """
                INSERT INTO user_row_filters
                    (id, datasource_id, subject_type, subject_id, field_name, allowed_values, created_at, updated_at, created_by)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                ON CONFLICT (datasource_id, subject_type, subject_id, lower(field_name))
                DO UPDATE SET
                    allowed_values = EXCLUDED.allowed_values,
                    updated_at = EXCLUDED.updated_at
                RETURNING id
                """;
        UUID savedId = jdbcTemplate.queryForObject(
                sql,
                UUID.class,
                id,
                request.datasourceId(),
                request.subjectType().trim().toUpperCase(),
                request.subjectId(),
                request.fieldName().trim(),
                valuesJson,
                now,
                now,
                actorId
        );
        return listRowFilters(request.datasourceId()).stream()
                .filter(item -> item.id().equals(savedId))
                .findFirst()
                .orElse(null);
    }

    public boolean deleteRowFilter(UUID id) {
        return jdbcTemplate.update("DELETE FROM user_row_filters WHERE id = ?", id) > 0;
    }

    private RowFilterDto mapRowFilterDto(ResultSet rs, int rowNum) throws SQLException {
        return new RowFilterDto(
                rs.getObject("id", UUID.class),
                rs.getObject("datasource_id", UUID.class),
                rs.getString("datasource_name"),
                rs.getString("subject_type"),
                rs.getObject("subject_id", UUID.class),
                rs.getString("subject_name"),
                rs.getString("field_name"),
                parseValues(rs.getString("allowed_values")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private List<String> parseValues(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
