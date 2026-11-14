package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.LabelDto;
import vn.edu.davs.backend.dto.WorkbookDto;
import vn.edu.davs.backend.dto.WorkbookVersionDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC persistence for UC08 Workbook Upload and UC10 Workbook Version History.
 */
@Repository
@RequiredArgsConstructor
public class JdbcWorkbookRepository {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcLabelRepository labelRepository;

    public Optional<WorkbookRow> findActiveByProjectAndName(UUID projectId, String name) {
        String sql = """
                SELECT w.id, w.project_id, p.name AS project_name, w.name, w.description,
                       w.current_version, w.owner_id,
                       COALESCE(NULLIF(u.full_name, ''), u.email, 'Hệ thống') AS owner_name,
                       w.import_status, w.needs_reconnect, w.created_at, w.updated_at,
                       COALESCE(v.size_bytes, 0) AS size_bytes,
                       COALESCE(v.file_name, w.name || '.twb') AS file_name
                FROM workbooks w
                INNER JOIN projects p ON p.id = w.project_id
                LEFT JOIN users u ON u.id = w.owner_id
                LEFT JOIN workbook_versions v
                  ON v.workbook_id = w.id AND v.version_number = w.current_version
                WHERE w.project_id = ? AND w.deleted_at IS NULL AND lower(w.name) = lower(?)
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapWorkbookRow(rs), projectId, name)
                .stream()
                .findFirst();
    }

    public Optional<WorkbookRow> findById(UUID workbookId) {
        String sql = """
                SELECT w.id, w.project_id, p.name AS project_name, w.name, w.description,
                       w.current_version, w.owner_id,
                       COALESCE(NULLIF(u.full_name, ''), u.email, 'Hệ thống') AS owner_name,
                       w.import_status, w.needs_reconnect, w.created_at, w.updated_at,
                       COALESCE(v.size_bytes, 0) AS size_bytes,
                       COALESCE(v.file_name, w.name || '.twb') AS file_name
                FROM workbooks w
                INNER JOIN projects p ON p.id = w.project_id
                LEFT JOIN users u ON u.id = w.owner_id
                LEFT JOIN workbook_versions v
                  ON v.workbook_id = w.id AND v.version_number = w.current_version
                WHERE w.id = ? AND w.deleted_at IS NULL
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapWorkbookRow(rs), workbookId)
                .stream()
                .findFirst();
    }

    public List<WorkbookRow> listByProject(UUID projectId) {
        String sql = """
                SELECT w.id, w.project_id, p.name AS project_name, w.name, w.description,
                       w.current_version, w.owner_id,
                       COALESCE(NULLIF(u.full_name, ''), u.email, 'Hệ thống') AS owner_name,
                       w.import_status, w.needs_reconnect, w.created_at, w.updated_at,
                       COALESCE(v.size_bytes, 0) AS size_bytes,
                       COALESCE(v.file_name, w.name || '.twb') AS file_name
                FROM workbooks w
                INNER JOIN projects p ON p.id = w.project_id
                LEFT JOIN users u ON u.id = w.owner_id
                LEFT JOIN workbook_versions v
                  ON v.workbook_id = w.id AND v.version_number = w.current_version
                WHERE w.project_id = ? AND w.deleted_at IS NULL
                ORDER BY w.updated_at DESC, lower(w.name) ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapWorkbookRow(rs), projectId);
    }

    public List<WorkbookRow> listAllActive() {
        String sql = """
                SELECT w.id, w.project_id, p.name AS project_name, w.name, w.description,
                       w.current_version, w.owner_id,
                       COALESCE(NULLIF(u.full_name, ''), u.email, 'Hệ thống') AS owner_name,
                       w.import_status, w.needs_reconnect, w.created_at, w.updated_at,
                       COALESCE(v.size_bytes, 0) AS size_bytes,
                       COALESCE(v.file_name, w.name || '.twb') AS file_name
                FROM workbooks w
                INNER JOIN projects p ON p.id = w.project_id
                LEFT JOIN users u ON u.id = w.owner_id
                LEFT JOIN workbook_versions v
                  ON v.workbook_id = w.id AND v.version_number = w.current_version
                WHERE w.deleted_at IS NULL AND p.deleted_at IS NULL
                ORDER BY w.updated_at DESC, lower(w.name) ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapWorkbookRow(rs));
    }

    public UUID insertWorkbook(
            UUID projectId,
            String name,
            String description,
            UUID ownerId,
            String definitionJson,
            boolean needsReconnect
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO workbooks (
                    project_id, name, description, current_version, owner_id,
                    definition, import_status, needs_reconnect
                )
                VALUES (?, ?, ?, 1, ?, ?::jsonb, 'IMPORTED', ?)
                RETURNING id
                """,
                UUID.class,
                projectId,
                name,
                description,
                ownerId,
                definitionJson != null ? definitionJson : "{}",
                needsReconnect
        );
    }

    public void updateWorkbookOnOverwrite(
            UUID workbookId,
            String description,
            int newVersionNumber,
            String definitionJson,
            boolean needsReconnect
    ) {
        jdbcTemplate.update(
                """
                UPDATE workbooks
                SET description = COALESCE(?, description),
                    current_version = ?,
                    definition = COALESCE(?::jsonb, definition),
                    needs_reconnect = ?,
                    updated_at = now()
                WHERE id = ? AND deleted_at IS NULL
                """,
                description,
                newVersionNumber,
                definitionJson,
                needsReconnect,
                workbookId
        );
    }

    public void updateCurrentVersion(UUID workbookId, int newVersionNumber, boolean needsReconnect) {
        jdbcTemplate.update(
                """
                UPDATE workbooks
                SET current_version = ?,
                    needs_reconnect = ?,
                    updated_at = now()
                WHERE id = ? AND deleted_at IS NULL
                """,
                newVersionNumber,
                needsReconnect,
                workbookId
        );
    }

    public UUID insertVersion(
            UUID workbookId,
            int versionNumber,
            String objectKey,
            String fileName,
            long sizeBytes,
            String note,
            UUID createdBy
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO workbook_versions (
                    workbook_id, version_number, object_key, file_name, size_bytes, note, created_by
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                UUID.class,
                workbookId,
                versionNumber,
                objectKey,
                fileName,
                sizeBytes,
                note,
                createdBy
        );
    }

    public List<VersionRow> listVersions(UUID workbookId, int currentVersion) {
        String sql = """
                SELECT v.id, v.workbook_id, v.version_number, v.object_key, v.file_name,
                       v.size_bytes, v.note, v.created_by,
                       COALESCE(NULLIF(u.full_name, ''), u.email, 'Hệ thống') AS created_by_name,
                       v.created_at, v.deleted_at, v.purged
                FROM workbook_versions v
                LEFT JOIN users u ON u.id = v.created_by
                WHERE v.workbook_id = ?
                ORDER BY v.version_number DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapVersionRow(rs, currentVersion), workbookId);
    }

    public Optional<VersionRow> findVersion(UUID workbookId, int versionNumber, int currentVersion) {
        String sql = """
                SELECT v.id, v.workbook_id, v.version_number, v.object_key, v.file_name,
                       v.size_bytes, v.note, v.created_by,
                       COALESCE(NULLIF(u.full_name, ''), u.email, 'Hệ thống') AS created_by_name,
                       v.created_at, v.deleted_at, v.purged
                FROM workbook_versions v
                LEFT JOIN users u ON u.id = v.created_by
                WHERE v.workbook_id = ? AND v.version_number = ?
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapVersionRow(rs, currentVersion),
                workbookId,
                versionNumber
        ).stream().findFirst();
    }

    public List<VersionRow> findActiveVersionsExceedingLimit(
            UUID workbookId,
            int currentVersion,
            int maxVersions
    ) {
        String sql = """
                SELECT v.id, v.workbook_id, v.version_number, v.object_key, v.file_name,
                       v.size_bytes, v.note, v.created_by,
                       COALESCE(NULLIF(u.full_name, ''), u.email, 'Hệ thống') AS created_by_name,
                       v.created_at, v.deleted_at, v.purged
                FROM workbook_versions v
                LEFT JOIN users u ON u.id = v.created_by
                WHERE v.workbook_id = ? AND v.deleted_at IS NULL AND v.purged = FALSE
                ORDER BY v.version_number DESC
                OFFSET ?
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapVersionRow(rs, currentVersion),
                workbookId,
                Math.max(1, maxVersions)
        );
    }

    public void markVersionPurged(UUID versionId) {
        jdbcTemplate.update(
                "UPDATE workbook_versions SET purged = TRUE, deleted_at = now() WHERE id = ?",
                versionId
        );
    }

    public void deleteVersionRow(UUID versionId) {
        jdbcTemplate.update("DELETE FROM workbook_versions WHERE id = ?", versionId);
    }

    public int countOtherVersionsUsingObjectKey(UUID workbookId, String objectKey, UUID excludeVersionId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM workbook_versions
                WHERE workbook_id = ? AND object_key = ? AND id <> ? AND deleted_at IS NULL AND purged = FALSE
                """,
                Integer.class,
                workbookId,
                objectKey,
                excludeVersionId
        );
        return count != null ? count : 0;
    }

    public WorkbookDto toDto(WorkbookRow row) {
        List<LabelDto> labels = labelRepository.listLabelsForTarget("WORKBOOK", row.id());
        return new WorkbookDto(
                row.id(),
                row.projectId(),
                row.projectName(),
                row.name(),
                row.description(),
                row.currentVersion(),
                row.ownerId(),
                row.ownerName(),
                row.importStatus(),
                row.needsReconnect(),
                row.sizeBytes(),
                row.fileName(),
                labels,
                row.createdAt(),
                row.updatedAt()
        );
    }

    private WorkbookRow mapWorkbookRow(ResultSet rs) throws SQLException {
        return new WorkbookRow(
                rs.getObject("id", UUID.class),
                rs.getObject("project_id", UUID.class),
                rs.getString("project_name"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("current_version"),
                rs.getObject("owner_id", UUID.class),
                rs.getString("owner_name"),
                rs.getString("import_status"),
                rs.getBoolean("needs_reconnect"),
                rs.getLong("size_bytes"),
                rs.getString("file_name"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private VersionRow mapVersionRow(ResultSet rs, int currentVersion) throws SQLException {
        int vNum = rs.getInt("version_number");
        boolean isPurged = rs.getBoolean("purged") || rs.getTimestamp("deleted_at") != null;
        return new VersionRow(
                rs.getObject("id", UUID.class),
                rs.getObject("workbook_id", UUID.class),
                vNum,
                rs.getString("object_key"),
                rs.getString("file_name"),
                rs.getLong("size_bytes"),
                rs.getString("note"),
                rs.getObject("created_by", UUID.class),
                rs.getString("created_by_name"),
                rs.getTimestamp("created_at").toInstant(),
                vNum == currentVersion,
                isPurged
        );
    }

    public record WorkbookRow(
            UUID id,
            UUID projectId,
            String projectName,
            String name,
            String description,
            int currentVersion,
            UUID ownerId,
            String ownerName,
            String importStatus,
            boolean needsReconnect,
            long sizeBytes,
            String fileName,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record VersionRow(
            UUID id,
            UUID workbookId,
            int versionNumber,
            String objectKey,
            String fileName,
            long sizeBytes,
            String note,
            UUID createdBy,
            String createdByName,
            Instant createdAt,
            boolean current,
            boolean purged
    ) {
        public WorkbookVersionDto toDto() {
            return new WorkbookVersionDto(
                    id,
                    workbookId,
                    versionNumber,
                    fileName,
                    sizeBytes,
                    note,
                    createdBy,
                    createdByName,
                    createdAt,
                    current,
                    purged
            );
        }
    }
}
