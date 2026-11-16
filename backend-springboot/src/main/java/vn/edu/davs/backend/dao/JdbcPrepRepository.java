package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.PrepFlowDto;
import vn.edu.davs.backend.dto.PrepRunDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * JDBC persistence for UC12 data preparation flows ({@code prep_flows}) and execution runs ({@code prep_runs}).
 */
@Repository
@RequiredArgsConstructor
public class JdbcPrepRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public List<PrepFlowDto> listFlows(UUID datasourceId) {
        if (datasourceId != null) {
            String sql = """
                    SELECT id, datasource_id, output_datasource_id, name, description,
                           steps_json::text AS steps_text, lineage_json::text AS lineage_text,
                           owner_id, published, created_at, updated_at
                    FROM prep_flows
                    WHERE datasource_id = ? AND deleted_at IS NULL
                    ORDER BY updated_at DESC
                    """;
            return jdbcTemplate.query(sql, (rs, rowNum) -> mapFlow(rs), datasourceId);
        }
        String sql = """
                SELECT id, datasource_id, output_datasource_id, name, description,
                       steps_json::text AS steps_text, lineage_json::text AS lineage_text,
                       owner_id, published, created_at, updated_at
                FROM prep_flows
                WHERE deleted_at IS NULL
                ORDER BY updated_at DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapFlow(rs));
    }

    public Optional<PrepFlowDto> findFlowById(UUID flowId) {
        if (flowId == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, datasource_id, output_datasource_id, name, description,
                       steps_json::text AS steps_text, lineage_json::text AS lineage_text,
                       owner_id, published, created_at, updated_at
                FROM prep_flows
                WHERE id = ? AND deleted_at IS NULL
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapFlow(rs), flowId).stream().findFirst();
    }

    public UUID insertFlow(
            UUID datasourceId,
            String name,
            String description,
            List<PrepFlowDto.PrepStepDto> steps,
            List<PrepFlowDto.LineageStepDto> lineage,
            UUID ownerId,
            boolean published
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO prep_flows (datasource_id, name, description, steps_json, lineage_json, owner_id, published)
                VALUES (?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?)
                RETURNING id
                """,
                UUID.class,
                datasourceId,
                name,
                description,
                toJson(steps),
                toJson(lineage),
                ownerId,
                published
        );
    }

    public void updateFlow(
            UUID flowId,
            UUID datasourceId,
            String name,
            String description,
            List<PrepFlowDto.PrepStepDto> steps,
            List<PrepFlowDto.LineageStepDto> lineage,
            boolean published
    ) {
        jdbcTemplate.update(
                """
                UPDATE prep_flows
                SET datasource_id = COALESCE(?, datasource_id),
                    name = ?,
                    description = ?,
                    steps_json = CAST(? AS jsonb),
                    lineage_json = CAST(? AS jsonb),
                    published = ?,
                    updated_at = now()
                WHERE id = ? AND deleted_at IS NULL
                """,
                datasourceId,
                name,
                description,
                toJson(steps),
                toJson(lineage),
                published,
                flowId
        );
    }

    public UUID insertRun(UUID flowId, UUID jobId, UUID createdBy) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO prep_runs (flow_id, job_id, status, created_by)
                VALUES (?, ?, 'PENDING', ?)
                RETURNING id
                """,
                UUID.class,
                flowId,
                jobId,
                createdBy
        );
    }

    public void markRunRunning(UUID runId) {
        jdbcTemplate.update("UPDATE prep_runs SET status = 'RUNNING' WHERE id = ?", runId);
    }

    public void completeRunSuccess(
            UUID runId,
            UUID flowId,
            long inputRows,
            long outputRows,
            long removedDuplicates,
            long replacedNulls,
            List<PrepFlowDto.LineageStepDto> lineage
    ) {
        jdbcTemplate.update(
                """
                UPDATE prep_runs
                SET status = 'SUCCEEDED',
                    input_rows = ?,
                    output_rows = ?,
                    removed_duplicates = ?,
                    replaced_nulls = ?,
                    lineage_json = CAST(? AS jsonb),
                    finished_at = now()
                WHERE id = ?
                """,
                inputRows,
                outputRows,
                removedDuplicates,
                replacedNulls,
                toJson(lineage),
                runId
        );
        jdbcTemplate.update(
                """
                UPDATE prep_flows
                SET lineage_json = CAST(? AS jsonb), published = TRUE, updated_at = now()
                WHERE id = ?
                """,
                toJson(lineage),
                flowId
        );
    }

    public void completeRunFailure(UUID runId, String errorCode, String errorMessage) {
        jdbcTemplate.update(
                """
                UPDATE prep_runs
                SET status = 'FAILED', error_code = ?, error_message = ?, finished_at = now()
                WHERE id = ?
                """,
                errorCode,
                errorMessage,
                runId
        );
    }

    public Optional<PrepRunDto> findRunById(UUID runId) {
        String sql = """
                SELECT id, flow_id, job_id, status, input_rows, output_rows,
                       removed_duplicates, replaced_nulls, lineage_json::text AS lineage_text,
                       error_code, error_message, started_at, finished_at
                FROM prep_runs
                WHERE id = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRun(rs), runId).stream().findFirst();
    }

    public List<PrepRunDto> listRuns(UUID flowId) {
        String sql = """
                SELECT id, flow_id, job_id, status, input_rows, output_rows,
                       removed_duplicates, replaced_nulls, lineage_json::text AS lineage_text,
                       error_code, error_message, started_at, finished_at
                FROM prep_runs
                WHERE flow_id = ?
                ORDER BY started_at DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRun(rs), flowId);
    }

    private PrepFlowDto mapFlow(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        List<PrepFlowDto.PrepStepDto> steps = fromJson(
                rs.getString("steps_text"),
                new TypeReference<List<PrepFlowDto.PrepStepDto>>() { }
        );
        List<PrepFlowDto.LineageStepDto> lineage = fromJson(
                rs.getString("lineage_text"),
                new TypeReference<List<PrepFlowDto.LineageStepDto>>() { }
        );
        return new PrepFlowDto(
                rs.getObject("id", UUID.class),
                rs.getObject("datasource_id", UUID.class),
                rs.getObject("output_datasource_id", UUID.class),
                rs.getString("name"),
                rs.getString("description"),
                steps,
                lineage,
                rs.getObject("owner_id", UUID.class),
                rs.getBoolean("published"),
                created != null ? created.toInstant() : null,
                updated != null ? updated.toInstant() : null
        );
    }

    private PrepRunDto mapRun(ResultSet rs) throws SQLException {
        Timestamp started = rs.getTimestamp("started_at");
        Timestamp finished = rs.getTimestamp("finished_at");
        List<PrepFlowDto.LineageStepDto> lineage = fromJson(
                rs.getString("lineage_text"),
                new TypeReference<List<PrepFlowDto.LineageStepDto>>() { }
        );
        return new PrepRunDto(
                rs.getObject("id", UUID.class),
                rs.getObject("flow_id", UUID.class),
                rs.getObject("job_id", UUID.class),
                rs.getString("status"),
                rs.getLong("input_rows"),
                rs.getLong("output_rows"),
                rs.getLong("removed_duplicates"),
                rs.getLong("replaced_nulls"),
                lineage,
                rs.getString("error_code"),
                rs.getString("error_message"),
                started != null ? started.toInstant() : null,
                finished != null ? finished.toInstant() : null
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize JSON", ex);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            if (json == null || json.isBlank()) {
                return objectMapper.readValue("[]", typeRef);
            }
            return objectMapper.readValue(json, typeRef);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse JSON", ex);
        }
    }
}
