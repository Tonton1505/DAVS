package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.DataModelDto;
import vn.edu.davs.backend.dto.DatasourceDto;
import vn.edu.davs.backend.dto.DependentWorkbookDto;
import vn.edu.davs.backend.dto.DiscoveredTableDto;
import vn.edu.davs.backend.dto.RefreshRunDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC persistence for UC11 (data_sources, connections, ds_tables),
 * UC13 (ds_model), and UC14 (extracts, refresh_schedules, refresh_runs, dependent workbooks).
 */
@Repository
@RequiredArgsConstructor
public class JdbcDatasourceRepository {

    private static final long EXTRACT_THRESHOLD_ROWS = 1_000_000L;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public List<DatasourceDto> listDatasources() {
        String sql = """
                SELECT ds.id, ds.name, ds.description, ds.connector_type, ds.mode, ds.status,
                       ds.owner_id, ds.project_id, ds.file_name, ds.row_count, ds.certified,
                       ds.quality_warning, ds.sensitivity, ds.last_refreshed_at, ds.created_at, ds.updated_at,
                       c.host, c.port, c.database_name, c.schema_name, c.username,
                       (c.encrypted_secret IS NOT NULL AND length(c.encrypted_secret) > 0) AS has_password,
                       rs.frequency, rs.run_time, rs.timezone, rs.refresh_type, rs.incremental_column,
                       rs.alert_emails, rs.enabled AS sched_enabled, rs.paused AS sched_paused,
                       rs.consecutive_failures, rs.next_run_at,
                       (SELECT COUNT(*) FROM workbooks w WHERE w.datasource_id = ds.id AND w.deleted_at IS NULL) AS dep_count
                FROM data_sources ds
                LEFT JOIN connections c ON c.datasource_id = ds.id
                LEFT JOIN refresh_schedules rs ON rs.datasource_id = ds.id
                WHERE ds.deleted_at IS NULL
                ORDER BY ds.updated_at DESC, lower(ds.name) ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapDatasourceDto(rs));
    }

    public Optional<DatasourceDto> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT ds.id, ds.name, ds.description, ds.connector_type, ds.mode, ds.status,
                       ds.owner_id, ds.project_id, ds.file_name, ds.row_count, ds.certified,
                       ds.quality_warning, ds.sensitivity, ds.last_refreshed_at, ds.created_at, ds.updated_at,
                       c.host, c.port, c.database_name, c.schema_name, c.username,
                       (c.encrypted_secret IS NOT NULL AND length(c.encrypted_secret) > 0) AS has_password,
                       rs.frequency, rs.run_time, rs.timezone, rs.refresh_type, rs.incremental_column,
                       rs.alert_emails, rs.enabled AS sched_enabled, rs.paused AS sched_paused,
                       rs.consecutive_failures, rs.next_run_at,
                       (SELECT COUNT(*) FROM workbooks w WHERE w.datasource_id = ds.id AND w.deleted_at IS NULL) AS dep_count
                FROM data_sources ds
                LEFT JOIN connections c ON c.datasource_id = ds.id
                LEFT JOIN refresh_schedules rs ON rs.datasource_id = ds.id
                WHERE ds.id = ? AND ds.deleted_at IS NULL
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapDatasourceDto(rs), id).stream().findFirst();
    }

    public Optional<String> findOwnerEmail(UUID datasourceId) {
        String sql = """
                SELECT u.email
                FROM data_sources ds
                JOIN users u ON u.id = ds.owner_id
                WHERE ds.id = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("email"), datasourceId).stream().findFirst();
    }

    public UUID insertDatasource(
            String name,
            String description,
            String connectorType,
            String mode,
            UUID ownerId,
            UUID projectId,
            String fileBucket,
            String fileObjectKey,
            String fileName,
            long rowCount
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO data_sources (
                    name, description, connector_type, mode, status, owner_id, project_id,
                    file_bucket, file_object_key, file_name, row_count, last_refreshed_at
                )
                VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?, ?, ?, ?, ?, CASE WHEN ? = 'EXTRACT' THEN now() ELSE NULL END)
                RETURNING id
                """,
                UUID.class,
                name,
                description,
                connectorType,
                mode,
                ownerId,
                projectId,
                fileBucket,
                fileObjectKey,
                fileName,
                rowCount,
                mode
        );
    }

    public void upsertConnection(
            UUID datasourceId,
            String host,
            Integer port,
            String databaseName,
            String schemaName,
            String username,
            String encryptedSecret
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO connections (datasource_id, host, port, database_name, schema_name, username, encrypted_secret)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (datasource_id) DO UPDATE SET
                    host = COALESCE(EXCLUDED.host, connections.host),
                    port = COALESCE(EXCLUDED.port, connections.port),
                    database_name = COALESCE(EXCLUDED.database_name, connections.database_name),
                    schema_name = COALESCE(EXCLUDED.schema_name, connections.schema_name),
                    username = COALESCE(EXCLUDED.username, connections.username),
                    encrypted_secret = COALESCE(EXCLUDED.encrypted_secret, connections.encrypted_secret),
                    updated_at = now()
                """,
                datasourceId,
                host,
                port,
                databaseName,
                schemaName != null && !schemaName.isBlank() ? schemaName : "public",
                username,
                encryptedSecret
        );
    }

    public Optional<String> getEncryptedSecret(UUID datasourceId) {
        return jdbcTemplate.query(
                "SELECT encrypted_secret FROM connections WHERE datasource_id = ?",
                (rs, rowNum) -> rs.getString("encrypted_secret"),
                datasourceId
        ).stream().findFirst();
    }

    public void saveDiscoveredTables(UUID datasourceId, List<DiscoveredTableDto> tables) {
        jdbcTemplate.update("DELETE FROM ds_tables WHERE datasource_id = ?", datasourceId);
        if (tables == null || tables.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO ds_tables (datasource_id, table_name, schema_name, row_count, columns_json, sample_rows_json)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb))
                """;
        for (DiscoveredTableDto table : tables) {
            jdbcTemplate.update(
                    sql,
                    datasourceId,
                    table.tableName(),
                    table.schemaName() != null && !table.schemaName().isBlank() ? table.schemaName() : "public",
                    table.rowCount(),
                    toJson(table.columns()),
                    toJson(table.previewRows())
            );
        }
    }

    public List<DiscoveredTableDto> findTablesByDatasource(UUID datasourceId) {
        String sql = """
                SELECT table_name, schema_name, row_count, columns_json::text AS cols_text, sample_rows_json::text AS rows_text
                FROM ds_tables
                WHERE datasource_id = ?
                ORDER BY lower(table_name) ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            long rowCount = rs.getLong("row_count");
            String suggested = rowCount > EXTRACT_THRESHOLD_ROWS ? "EXTRACT" : "LIVE";
            List<DiscoveredTableDto.ColumnSchemaDto> cols = fromJson(
                    rs.getString("cols_text"),
                    new TypeReference<List<DiscoveredTableDto.ColumnSchemaDto>>() { }
            );
            List<Map<String, Object>> rows = fromJson(
                    rs.getString("rows_text"),
                    new TypeReference<List<Map<String, Object>>>() { }
            );
            return new DiscoveredTableDto(
                    rs.getString("table_name"),
                    rs.getString("schema_name"),
                    rowCount,
                    suggested,
                    cols,
                    rows
            );
        }, datasourceId);
    }

    public void upsertDataModel(
            UUID datasourceId,
            List<DataModelDto.ModelTableDto> tables,
            List<DataModelDto.ModelRelationshipDto> relationships,
            List<DataModelDto.ModelFieldDto> fields,
            UUID updatedBy
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO ds_model (datasource_id, tables_json, relationships_json, fields_json, updated_at, updated_by)
                VALUES (?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), now(), ?)
                ON CONFLICT (datasource_id) DO UPDATE SET
                    tables_json = EXCLUDED.tables_json,
                    relationships_json = EXCLUDED.relationships_json,
                    fields_json = EXCLUDED.fields_json,
                    updated_at = now(),
                    updated_by = EXCLUDED.updated_by
                """,
                datasourceId,
                toJson(tables),
                toJson(relationships),
                toJson(fields),
                updatedBy
        );
    }

    public Optional<StoredModelRecord> findDataModel(UUID datasourceId) {
        String sql = """
                SELECT tables_json::text AS t_text, relationships_json::text AS r_text,
                       fields_json::text AS f_text, updated_at
                FROM ds_model
                WHERE datasource_id = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp updated = rs.getTimestamp("updated_at");
            return new StoredModelRecord(
                    fromJson(rs.getString("t_text"), new TypeReference<List<DataModelDto.ModelTableDto>>() { }),
                    fromJson(rs.getString("r_text"), new TypeReference<List<DataModelDto.ModelRelationshipDto>>() { }),
                    fromJson(rs.getString("f_text"), new TypeReference<List<DataModelDto.ModelFieldDto>>() { }),
                    updated != null ? updated.toInstant() : Instant.now()
            );
        }, datasourceId).stream().findFirst();
    }

    public void updateMode(UUID datasourceId, String mode) {
        jdbcTemplate.update(
                "UPDATE data_sources SET mode = ?, updated_at = now() WHERE id = ? AND deleted_at IS NULL",
                mode,
                datasourceId
        );
    }

    public void upsertExtractSnapshot(UUID datasourceId, long rowCount, List<Map<String, Object>> snapshotRows) {
        jdbcTemplate.update(
                """
                INSERT INTO extracts (datasource_id, row_count, snapshot_json, version, last_refreshed_at, updated_at)
                VALUES (?, ?, CAST(? AS jsonb), 1, now(), now())
                ON CONFLICT (datasource_id) DO UPDATE SET
                    row_count = EXCLUDED.row_count,
                    snapshot_json = EXCLUDED.snapshot_json,
                    version = extracts.version + 1,
                    last_refreshed_at = now(),
                    updated_at = now()
                """,
                datasourceId,
                rowCount,
                toJson(snapshotRows)
        );
        jdbcTemplate.update(
                "UPDATE data_sources SET row_count = ?, last_refreshed_at = now(), status = 'ACTIVE', updated_at = now() WHERE id = ?",
                rowCount,
                datasourceId
        );
    }

    public Optional<ExtractSnapshotRecord> findExtractSnapshot(UUID datasourceId) {
        String sql = """
                SELECT row_count, version, snapshot_json::text AS snap_text, last_refreshed_at
                FROM extracts
                WHERE datasource_id = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp ts = rs.getTimestamp("last_refreshed_at");
            return new ExtractSnapshotRecord(
                    rs.getLong("row_count"),
                    rs.getInt("version"),
                    fromJson(rs.getString("snap_text"), new TypeReference<List<Map<String, Object>>>() { }),
                    ts != null ? ts.toInstant() : null
            );
        }, datasourceId).stream().findFirst();
    }

    public void upsertRefreshSchedule(
            UUID datasourceId,
            String frequency,
            String runTime,
            String timezone,
            String refreshType,
            String incrementalColumn,
            String alertEmails,
            boolean enabled,
            UUID updatedBy
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO refresh_schedules (
                    datasource_id, frequency, run_time, timezone, refresh_type,
                    incremental_column, alert_emails, enabled, paused, consecutive_failures, updated_at, updated_by
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, FALSE, 0, now(), ?)
                ON CONFLICT (datasource_id) DO UPDATE SET
                    frequency = EXCLUDED.frequency,
                    run_time = EXCLUDED.run_time,
                    timezone = EXCLUDED.timezone,
                    refresh_type = EXCLUDED.refresh_type,
                    incremental_column = EXCLUDED.incremental_column,
                    alert_emails = EXCLUDED.alert_emails,
                    enabled = EXCLUDED.enabled,
                    paused = CASE WHEN EXCLUDED.enabled THEN FALSE ELSE refresh_schedules.paused END,
                    consecutive_failures = CASE WHEN EXCLUDED.enabled THEN 0 ELSE refresh_schedules.consecutive_failures END,
                    updated_at = now(),
                    updated_by = EXCLUDED.updated_by
                """,
                datasourceId,
                frequency,
                runTime,
                timezone,
                refreshType,
                incrementalColumn,
                alertEmails,
                enabled,
                updatedBy
        );
    }

    public int countRunningRefreshes() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_runs WHERE status = 'RUNNING'",
                Integer.class
        );
        return count != null ? count : 0;
    }

    public UUID insertRefreshRun(
            UUID datasourceId,
            UUID jobId,
            String refreshType,
            String triggerType,
            String initialStatus,
            UUID triggeredBy
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO refresh_runs (datasource_id, job_id, refresh_type, trigger_type, status, triggered_by)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                UUID.class,
                datasourceId,
                jobId,
                refreshType,
                triggerType,
                initialStatus,
                triggeredBy
        );
    }

    public void markRefreshRunStatus(UUID runId, String status) {
        jdbcTemplate.update(
                "UPDATE refresh_runs SET status = ? WHERE id = ?",
                status,
                runId
        );
    }

    public void completeRefreshRunSuccess(UUID runId, UUID datasourceId, long rowsProcessed) {
        jdbcTemplate.update(
                """
                UPDATE refresh_runs
                SET status = 'SUCCEEDED', rows_processed = ?, finished_at = now()
                WHERE id = ?
                """,
                rowsProcessed,
                runId
        );
        jdbcTemplate.update(
                """
                UPDATE refresh_schedules
                SET consecutive_failures = 0, paused = FALSE, updated_at = now()
                WHERE datasource_id = ?
                """,
                datasourceId
        );
        jdbcTemplate.update(
                "UPDATE data_sources SET status = 'ACTIVE', last_refreshed_at = now(), updated_at = now() WHERE id = ?",
                datasourceId
        );
    }

    public int recordRefreshFailureAndGetConsecutiveCount(
            UUID runId,
            UUID datasourceId,
            String errorCode,
            String errorMessage,
            int pauseThreshold
    ) {
        jdbcTemplate.update(
                """
                UPDATE refresh_runs
                SET status = 'FAILED', error_code = ?, error_message = ?, finished_at = now()
                WHERE id = ?
                """,
                errorCode,
                errorMessage,
                runId
        );

        // Ensure schedule row exists to track consecutive failures
        jdbcTemplate.update(
                """
                INSERT INTO refresh_schedules (datasource_id, frequency, run_time, timezone, refresh_type, enabled, consecutive_failures)
                VALUES (?, 'DAILY', '02:00', 'Asia/Ho_Chi_Minh', 'FULL', TRUE, 0)
                ON CONFLICT (datasource_id) DO NOTHING
                """,
                datasourceId
        );

        Integer nextFailures = jdbcTemplate.queryForObject(
                """
                UPDATE refresh_schedules
                SET consecutive_failures = consecutive_failures + 1,
                    paused = CASE WHEN consecutive_failures + 1 >= ? THEN TRUE ELSE paused END,
                    enabled = CASE WHEN consecutive_failures + 1 >= ? THEN FALSE ELSE enabled END,
                    updated_at = now()
                WHERE datasource_id = ?
                RETURNING consecutive_failures
                """,
                Integer.class,
                pauseThreshold,
                pauseThreshold,
                datasourceId
        );
        int failures = nextFailures != null ? nextFailures : 1;
        String dsStatus = failures >= pauseThreshold ? "PAUSED" : "FAILED";
        jdbcTemplate.update(
                "UPDATE data_sources SET status = ?, updated_at = now() WHERE id = ?",
                dsStatus,
                datasourceId
        );
        return failures;
    }

    public Optional<RefreshRunDto> findRefreshRunById(UUID runId) {
        String sql = """
                SELECT id, datasource_id, job_id, refresh_type, trigger_type, status,
                       rows_processed, error_code, error_message, started_at, finished_at
                FROM refresh_runs
                WHERE id = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRefreshRun(rs), runId).stream().findFirst();
    }

    public List<RefreshRunDto> listRefreshRuns(UUID datasourceId) {
        String sql = """
                SELECT id, datasource_id, job_id, refresh_type, trigger_type, status,
                       rows_processed, error_code, error_message, started_at, finished_at
                FROM refresh_runs
                WHERE datasource_id = ?
                ORDER BY started_at DESC
                LIMIT 50
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRefreshRun(rs), datasourceId);
    }

    public List<DependentWorkbookDto> listDependentWorkbooks(UUID datasourceId) {
        String sql = """
                SELECT id, name, project_id, owner_id, status, updated_at
                FROM workbooks
                WHERE datasource_id = ? AND deleted_at IS NULL
                ORDER BY lower(name) ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp updated = rs.getTimestamp("updated_at");
            return new DependentWorkbookDto(
                    rs.getObject("id", UUID.class),
                    rs.getString("name"),
                    rs.getObject("project_id", UUID.class),
                    rs.getObject("owner_id", UUID.class),
                    rs.getString("status"),
                    updated != null ? updated.toInstant() : null
            );
        }, datasourceId);
    }

    private static DatasourceDto mapDatasourceDto(ResultSet rs) throws SQLException {
        long rowCount = rs.getLong("row_count");
        String suggestedMode = rowCount > EXTRACT_THRESHOLD_ROWS ? "EXTRACT" : "LIVE";
        Timestamp lastRefreshed = rs.getTimestamp("last_refreshed_at");
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");

        DatasourceDto.ConnectionSummaryDto connection = null;
        String host = rs.getString("host");
        String dbName = rs.getString("database_name");
        String username = rs.getString("username");
        boolean hasPassword = rs.getBoolean("has_password");
        if (host != null || dbName != null || username != null || hasPassword) {
            Integer port = rs.getObject("port") != null ? rs.getInt("port") : null;
            connection = new DatasourceDto.ConnectionSummaryDto(
                    host,
                    port,
                    dbName,
                    rs.getString("schema_name"),
                    username,
                    hasPassword
            );
        }

        DatasourceDto.RefreshScheduleDto schedule = null;
        String frequency = rs.getString("frequency");
        if (frequency != null) {
            Timestamp nextRun = rs.getTimestamp("next_run_at");
            schedule = new DatasourceDto.RefreshScheduleDto(
                    frequency,
                    rs.getString("run_time"),
                    rs.getString("timezone"),
                    rs.getString("refresh_type"),
                    rs.getString("incremental_column"),
                    rs.getString("alert_emails"),
                    rs.getBoolean("sched_enabled"),
                    rs.getBoolean("sched_paused"),
                    rs.getInt("consecutive_failures"),
                    nextRun != null ? nextRun.toInstant() : null
            );
        }

        return new DatasourceDto(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("connector_type"),
                rs.getString("mode"),
                suggestedMode,
                rs.getString("status"),
                rs.getObject("owner_id", UUID.class),
                rs.getObject("project_id", UUID.class),
                rs.getString("file_name"),
                rowCount,
                rs.getBoolean("certified"),
                rs.getString("quality_warning"),
                rs.getString("sensitivity"),
                rs.getInt("dep_count"),
                connection,
                schedule,
                lastRefreshed != null ? lastRefreshed.toInstant() : null,
                created != null ? created.toInstant() : null,
                updated != null ? updated.toInstant() : null
        );
    }

    private static RefreshRunDto mapRefreshRun(ResultSet rs) throws SQLException {
        Timestamp started = rs.getTimestamp("started_at");
        Timestamp finished = rs.getTimestamp("finished_at");
        return new RefreshRunDto(
                rs.getObject("id", UUID.class),
                rs.getObject("datasource_id", UUID.class),
                rs.getObject("job_id", UUID.class),
                rs.getString("refresh_type"),
                rs.getString("trigger_type"),
                rs.getString("status"),
                rs.getLong("rows_processed"),
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

    public record StoredModelRecord(
            List<DataModelDto.ModelTableDto> tables,
            List<DataModelDto.ModelRelationshipDto> relationships,
            List<DataModelDto.ModelFieldDto> fields,
            Instant updatedAt
    ) {
    }

    public record ExtractSnapshotRecord(
            long rowCount,
            int version,
            List<Map<String, Object>> rows,
            Instant lastRefreshedAt
    ) {
    }
}
