package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.DataModelDto;
import vn.edu.davs.backend.dto.DatasourceDto;
import vn.edu.davs.backend.dto.DependentWorkbookDto;
import vn.edu.davs.backend.dto.DiscoveredTableDto;
import vn.edu.davs.backend.dto.RefreshRunDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC persistence for UC11 (data_sources, connections, ds_tables),
 * UC13 (ds_model), and UC14 (extracts, refresh_schedules, refresh_runs, dependent workbooks).
 */
public interface IDatasourceDao {


    List<DatasourceDto> listDatasources();


    Optional<DatasourceDto> findById(UUID id);


    Optional<String> findOwnerEmail(UUID datasourceId);


    UUID insertDatasource(
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
    );


    void upsertConnection(
            UUID datasourceId,
            String host,
            Integer port,
            String databaseName,
            String schemaName,
            String username,
            String encryptedSecret
    );


    Optional<String> getEncryptedSecret(UUID datasourceId);


    void saveDiscoveredTables(UUID datasourceId, List<DiscoveredTableDto> tables);


    List<DiscoveredTableDto> findTablesByDatasource(UUID datasourceId);


    void upsertDataModel(
            UUID datasourceId,
            List<DataModelDto.ModelTableDto> tables,
            List<DataModelDto.ModelRelationshipDto> relationships,
            List<DataModelDto.ModelFieldDto> fields,
            UUID updatedBy
    );


    Optional<StoredModelRecord> findDataModel(UUID datasourceId);


    void updateMode(UUID datasourceId, String mode);


    void upsertExtractSnapshot(UUID datasourceId, long rowCount, List<Map<String, Object>> snapshotRows);


    Optional<ExtractSnapshotRecord> findExtractSnapshot(UUID datasourceId);


    void upsertRefreshSchedule(
            UUID datasourceId,
            String frequency,
            String runTime,
            String timezone,
            String refreshType,
            String incrementalColumn,
            String alertEmails,
            boolean enabled,
            UUID updatedBy
    );


    int countRunningRefreshes();


    UUID insertRefreshRun(
            UUID datasourceId,
            UUID jobId,
            String refreshType,
            String triggerType,
            String initialStatus,
            UUID triggeredBy
    );


    void markRefreshRunStatus(UUID runId, String status);


    void completeRefreshRunSuccess(UUID runId, UUID datasourceId, long rowsProcessed);


    int recordRefreshFailureAndGetConsecutiveCount(
            UUID runId,
            UUID datasourceId,
            String errorCode,
            String errorMessage,
            int pauseThreshold
    );


    Optional<RefreshRunDto> findRefreshRunById(UUID runId);


    List<RefreshRunDto> listRefreshRuns(UUID datasourceId);


    List<DependentWorkbookDto> listDependentWorkbooks(UUID datasourceId);


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
