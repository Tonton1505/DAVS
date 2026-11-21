package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.IDatasourceDao;
import vn.edu.davs.backend.service.impl.DatasourceAndModelServiceImpl;
import vn.edu.davs.backend.dto.ConnectorDto;
import vn.edu.davs.backend.dto.CreateDatasourceRequest;
import vn.edu.davs.backend.dto.DataModelDto;
import vn.edu.davs.backend.dto.DatasourceDto;
import vn.edu.davs.backend.dto.DependentWorkbookDto;
import vn.edu.davs.backend.dto.DiscoveredTableDto;
import vn.edu.davs.backend.dto.ModelPreviewResponse;
import vn.edu.davs.backend.dto.RefreshRunDto;
import vn.edu.davs.backend.dto.SaveDataModelRequest;
import vn.edu.davs.backend.dto.TestConnectionRequest;
import vn.edu.davs.backend.dto.TestConnectionResponse;
import vn.edu.davs.backend.dto.UpdateConnectionRequest;
import vn.edu.davs.backend.dto.UpdateRefreshScheduleRequest;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Core business service for UC11 (Connectors &amp; Data Source Creation),
 * UC13 (Semantic Data Modeling), and UC14 (Extract Management &amp; Refresh Scheduling).
 */
public interface DatasourceAndModelService {


    public static final String REFRESH_EXTRACT_JOB_TYPE = "REFRESH_EXTRACT";


    List<ConnectorDto> listConnectors();


    TestConnectionResponse testConnection(TestConnectionRequest request);


    @Transactional
    DatasourceDto createDatasource(CreateDatasourceRequest request, DavsUser actor);


    @Transactional
    DatasourceDto createDatasourceFromFile(
            MultipartFile file,
            String name,
            String connectorType,
            String mode,
            UUID projectId,
            DavsUser actor
    );


    @Transactional(readOnly = true)
    List<DatasourceDto> listDatasources();


    @Transactional(readOnly = true)
    DatasourceDto getDatasource(UUID id);


    @Transactional(readOnly = true)
    List<DiscoveredTableDto> listTables(UUID datasourceId);


    // --- UC13: Semantic Data Model ---

    @Transactional(readOnly = true)
    DataModelDto getDataModel(UUID datasourceId);


    @Transactional
    DataModelDto saveDataModel(UUID datasourceId, SaveDataModelRequest request, DavsUser actor);


    @Transactional(readOnly = true)
    List<DataModelDto.ModelRelationshipDto> suggestRelationships(UUID datasourceId);


    @Transactional(readOnly = true)
    ModelPreviewResponse previewDataModel(UUID datasourceId, SaveDataModelRequest request);


    // --- UC14: Mode Switch, Refresh Schedule, Extract Refresh, Connection Update & Dependents ---

    @Transactional
    DatasourceDto updateMode(UUID datasourceId, String rawMode, DavsUser actor);


    @Transactional
    DatasourceDto updateSchedule(UUID datasourceId, UpdateRefreshScheduleRequest request, DavsUser actor);


    @Transactional
    RefreshRunDto enqueueManualRefresh(UUID datasourceId, boolean simulateFailure, DavsUser actor);


    @Transactional
    RefreshRunDto executeRefreshRun(UUID runId, boolean simulateFailure);


    @Transactional(readOnly = true)
    List<RefreshRunDto> listRefreshRuns(UUID datasourceId);


    @Transactional
    DatasourceDto updateConnection(UUID datasourceId, UpdateConnectionRequest request, DavsUser actor);


    @Transactional(readOnly = true)
    List<DependentWorkbookDto> listDependents(UUID datasourceId);


    @Transactional(readOnly = true)
    IDatasourceDao.ExtractSnapshotRecord getExtractSnapshot(UUID datasourceId);


    // --- Helper methods for CSV parsing, type inference, and BUS-DAT-07/08/09 ---

    static String inferDefaultRole(String columnName, String dataType) {
        return DatasourceAndModelServiceImpl.inferDefaultRole(columnName, dataType);
    }

    static boolean areTypesCompatible(String leftType, String rightType) {
        return DatasourceAndModelServiceImpl.areTypesCompatible(leftType, rightType);
    }

    static String normalizeDataType(String rawType) {
        return DatasourceAndModelServiceImpl.normalizeDataType(rawType);
    }

    static DiscoveredTableDto parseCsvToTable(String tableName, String csvContent) {
        return DatasourceAndModelServiceImpl.parseCsvToTable(tableName, csvContent);
    }
}
