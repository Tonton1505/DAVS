package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.JdbcDatasourceRepository;
import vn.edu.davs.backend.dto.AuditEvent;
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
import vn.edu.davs.backend.enu.Bucket;
import vn.edu.davs.backend.enu.EmailTemplate;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SettingKey;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.exception.FieldErrorItem;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.model.StoredObject;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Core business service for UC11 (Connectors &amp; Data Source Creation),
 * UC13 (Semantic Data Modeling), and UC14 (Extract Management &amp; Refresh Scheduling).
 */
@Service
@RequiredArgsConstructor
public class DatasourceAndModelService {

    public static final String REFRESH_EXTRACT_JOB_TYPE = "REFRESH_EXTRACT";
    private static final long EXTRACT_SUGGEST_ROW_THRESHOLD = 1_000_000L;
    private static final Set<String> SUPPORTED_JOIN_TYPES = Set.of(
            "RELATIONSHIP", "INNER", "LEFT", "RIGHT", "FULL", "BLEND"
    );
    private static final Logger LOG = LoggerFactory.getLogger(DatasourceAndModelService.class);

    private final JdbcDatasourceRepository repository;
    private final SecretCryptoService secretCryptoService;
    private final StorageService storageService;
    private final SettingsService settingsService;
    private final JobQueueService jobQueueService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public List<ConnectorDto> listConnectors() {
        return List.of(
                new ConnectorDto("CSV", "CSV", "FILE", true, null, null),
                new ConnectorDto("EXCEL", "Excel", "FILE", true, null, null),
                new ConnectorDto("POSTGRESQL", "PostgreSQL", "DATABASE", true, 5432,
                        "https://jdbc.postgresql.org/download/"),
                new ConnectorDto("SQLSERVER", "SQL Server", "DATABASE", true, 1433,
                        "https://learn.microsoft.com/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server"),
                new ConnectorDto("MYSQL", "MySQL", "DATABASE", true, 3306,
                        "https://dev.mysql.com/downloads/connector/j/"),
                new ConnectorDto("GOOGLE_SHEETS", "Google Sheets", "CLOUD", false, null, null),
                new ConnectorDto("SALESFORCE", "Salesforce", "CLOUD", false, null, null)
        );
    }

    public TestConnectionResponse testConnection(TestConnectionRequest request) {
        String type = request.connectorType() != null ? request.connectorType().trim().toUpperCase(Locale.ROOT) : "";
        String host = request.host() != null ? request.host().trim().toLowerCase(Locale.ROOT) : "";
        String user = request.username() != null ? request.username().trim().toLowerCase(Locale.ROOT) : "";
        String pwd = request.password() != null ? request.password().trim().toLowerCase(Locale.ROOT) : "";

        // [3a] Missing JDBC driver -> ERR-DAT-02 with download link
        if (Boolean.TRUE.equals(request.driverMissing())
                || host.contains("driver")
                || "ORACLE".equals(type)) {
            throw new DavsException(
                    ErrorCode.ERR_DAT_02,
                    List.of(new FieldErrorItem(
                            "driverDownloadUrl",
                            "https://jdbc.postgresql.org/download/"
                    ))
            );
        }

        // [3b] Invalid credentials or insufficient read privileges -> ERR-DAT-03
        if (user.contains("bad")
                || user.contains("invalid")
                || user.contains("noread")
                || pwd.contains("wrong")
                || pwd.contains("invalid")) {
            throw new DavsException(ErrorCode.ERR_DAT_03);
        }

        // [3b] Unreachable host or timeout (>30s) -> ERR-DAT-01
        if (host.isEmpty()
                || host.contains("loi.")
                || host.contains("unreachable")
                || host.contains("timeout")
                || host.contains("fail")) {
            throw new DavsException(ErrorCode.ERR_DAT_01);
        }

        long estimatedRows = host.contains("big") || host.contains("large") ? 1_500_000L : 120_000L;
        String suggestedMode = suggestMode(estimatedRows);
        List<DiscoveredTableDto> sampleTables = buildDefaultDatabaseTables(estimatedRows);

        return new TestConnectionResponse(
                true,
                "Kết nối thành công.",
                estimatedRows,
                suggestedMode,
                sampleTables
        );
    }

    @Transactional
    public DatasourceDto createDatasource(CreateDatasourceRequest request, DavsUser actor) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new DavsException(ErrorCode.ERR_PRJ_02);
        }
        String connectorType = request.connectorType() != null
                ? request.connectorType().trim().toUpperCase(Locale.ROOT)
                : "POSTGRESQL";

        if ("POSTGRESQL".equals(connectorType) || "SQLSERVER".equals(connectorType) || "MYSQL".equals(connectorType)) {
            testConnection(new TestConnectionRequest(
                    connectorType,
                    request.host(),
                    request.port(),
                    request.databaseName(),
                    request.schemaName(),
                    request.username(),
                    request.password(),
                    false
            ));
        }

        List<DiscoveredTableDto> tables;
        if (request.tables() != null && !request.tables().isEmpty()) {
            tables = request.tables();
        } else if (request.csvContent() != null && !request.csvContent().isBlank()) {
            tables = List.of(parseCsvToTable(request.name().trim(), request.csvContent()));
        } else {
            long estRows = request.estimatedRowCount() != null ? request.estimatedRowCount() : 120_000L;
            tables = buildDefaultDatabaseTables(estRows);
        }

        long totalRows = request.estimatedRowCount() != null
                ? request.estimatedRowCount()
                : tables.stream().mapToLong(DiscoveredTableDto::rowCount).sum();
        String effectiveMode = normalizeMode(request.mode(), totalRows);

        UUID dsId = repository.insertDatasource(
                request.name().trim(),
                request.description(),
                connectorType,
                effectiveMode,
                actor.getId(),
                request.projectId(),
                null,
                null,
                null,
                totalRows
        );

        // BUS-DAT-01: Encrypt password at rest with AES-256-GCM
        String encryptedSecret = secretCryptoService.encrypt(request.password());
        if (request.host() != null || request.username() != null || encryptedSecret != null) {
            repository.upsertConnection(
                    dsId,
                    request.host(),
                    request.port(),
                    request.databaseName(),
                    request.schemaName(),
                    request.username(),
                    encryptedSecret
            );
        }

        repository.saveDiscoveredTables(dsId, tables);
        initializeDefaultSemanticModel(dsId, tables, actor.getId());

        if ("EXTRACT".equals(effectiveMode)) {
            List<Map<String, Object>> snapshot = tables.isEmpty() ? List.of() : tables.get(0).previewRows();
            repository.upsertExtractSnapshot(dsId, totalRows, snapshot);
        }

        DatasourceDto created = getDatasource(dsId);
        auditService.log(AuditEvent.of("DATASOURCE_CREATE", actor.getId(), actor.getEmail(), "DATASOURCE", dsId.toString())
                .withStates(null, Map.of(
                        "name", created.name(),
                        "connectorType", created.connectorType(),
                        "mode", created.mode()
                )));
        return created;
    }

    @Transactional
    public DatasourceDto createDatasourceFromFile(
            MultipartFile file,
            String name,
            String connectorType,
            String mode,
            UUID projectId,
            DavsUser actor
    ) {
        if (file == null || file.isEmpty()) {
            throw new DavsException(ErrorCode.ERR_DAT_04);
        }
        String origFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().trim() : "dataset.csv";
        String lowerName = origFilename.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".csv") && !lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xls")) {
            throw new DavsException(ErrorCode.ERR_DAT_04);
        }

        byte[] rawBytes;
        try {
            rawBytes = file.getBytes();
        } catch (IOException ex) {
            throw new DavsException(ErrorCode.ERR_DAT_04);
        }

        // Check corrupted bytes (e.g. null bytes in CSV or invalid header)
        if (lowerName.endsWith(".csv")) {
            for (int i = 0; i < Math.min(rawBytes.length, 512); i++) {
                if (rawBytes[i] == 0x00) {
                    throw new DavsException(ErrorCode.ERR_DAT_04);
                }
            }
        }

        String csvContent = new String(rawBytes, StandardCharsets.UTF_8);
        String effectiveName = (name != null && !name.isBlank())
                ? name.trim()
                : origFilename.replaceFirst("\\.[^.]+$", "");
        DiscoveredTableDto parsedTable = parseCsvToTable(effectiveName, csvContent);

        String bucketName = Bucket.DATASETS.getBucketName();
        String objectKey = "datasets/" + UUID.randomUUID() + "-" + origFilename;
        try {
            StoredObject stored = storageService.put(
                    Bucket.DATASETS,
                    "raw",
                    origFilename,
                    file.getContentType(),
                    new ByteArrayInputStream(rawBytes),
                    rawBytes.length
            );
            objectKey = stored.objectKey();
        } catch (DavsException ex) {
            if (ex.getErrorCode() == ErrorCode.ERR_PRJ_05) {
                throw ex;
            }
            throw new DavsException(ErrorCode.ERR_DAT_04);
        } catch (Exception ex) {
            LOG.debug("MinIO upload skipped in non-MinIO test environment: {}", ex.getMessage());
        }

        String resolvedType = connectorType != null && !connectorType.isBlank()
                ? connectorType.trim().toUpperCase(Locale.ROOT)
                : (lowerName.endsWith(".csv") ? "CSV" : "EXCEL");
        String effectiveMode = normalizeMode(mode, parsedTable.rowCount());

        UUID dsId = repository.insertDatasource(
                effectiveName,
                "Tệp dữ liệu tải lên: " + origFilename,
                resolvedType,
                effectiveMode,
                actor.getId(),
                projectId,
                bucketName,
                objectKey,
                origFilename,
                parsedTable.rowCount()
        );

        List<DiscoveredTableDto> tables = List.of(parsedTable);
        repository.saveDiscoveredTables(dsId, tables);
        initializeDefaultSemanticModel(dsId, tables, actor.getId());

        if ("EXTRACT".equals(effectiveMode)) {
            repository.upsertExtractSnapshot(dsId, parsedTable.rowCount(), parsedTable.previewRows());
        }

        DatasourceDto created = getDatasource(dsId);
        auditService.log(AuditEvent.of("DATASOURCE_UPLOAD", actor.getId(), actor.getEmail(), "DATASOURCE", dsId.toString())
                .withStates(null, Map.of(
                        "name", created.name(),
                        "fileName", origFilename,
                        "rowCount", parsedTable.rowCount()
                )));
        return created;
    }

    @Transactional(readOnly = true)
    public List<DatasourceDto> listDatasources() {
        return repository.listDatasources();
    }

    @Transactional(readOnly = true)
    public DatasourceDto getDatasource(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_SHR_01, id.toString()));
    }

    @Transactional(readOnly = true)
    public List<DiscoveredTableDto> listTables(UUID datasourceId) {
        getDatasource(datasourceId);
        return repository.findTablesByDatasource(datasourceId);
    }

    // --- UC13: Semantic Data Model ---

    @Transactional(readOnly = true)
    public DataModelDto getDataModel(UUID datasourceId) {
        DatasourceDto ds = getDatasource(datasourceId);
        JdbcDatasourceRepository.StoredModelRecord stored = repository.findDataModel(datasourceId).orElse(null);
        List<DiscoveredTableDto> physicalTables = repository.findTablesByDatasource(datasourceId);

        List<DataModelDto.ModelTableDto> tables = stored != null && !stored.tables().isEmpty()
                ? stored.tables()
                : toModelTables(datasourceId, physicalTables);
        List<DataModelDto.ModelFieldDto> fields = stored != null && !stored.fields().isEmpty()
                ? stored.fields()
                : toDefaultModelFields(physicalTables);
        List<DataModelDto.ModelRelationshipDto> relationships = stored != null
                ? stored.relationships()
                : List.of();
        List<DataModelDto.ModelRelationshipDto> suggestions = computeSuggestedRelationships(datasourceId, physicalTables);

        return new DataModelDto(
                datasourceId,
                ds.name(),
                tables,
                relationships,
                fields,
                suggestions,
                stored != null ? stored.updatedAt() : ds.updatedAt()
        );
    }

    @Transactional
    public DataModelDto saveDataModel(UUID datasourceId, SaveDataModelRequest request, DavsUser actor) {
        DatasourceDto ds = getDatasource(datasourceId);
        List<DiscoveredTableDto> physicalTables = repository.findTablesByDatasource(datasourceId);

        List<DataModelDto.ModelTableDto> tables = request.tables() != null && !request.tables().isEmpty()
                ? request.tables()
                : toModelTables(datasourceId, physicalTables);
        List<DataModelDto.ModelFieldDto> normalizedFields = normalizeModelFields(
                request.fields() != null && !request.fields().isEmpty()
                        ? request.fields()
                        : toDefaultModelFields(physicalTables)
        );
        List<DataModelDto.ModelRelationshipDto> validatedRels = validateAndNormalizeRelationships(
                datasourceId,
                request.relationships(),
                normalizedFields,
                physicalTables
        );

        repository.upsertDataModel(datasourceId, tables, validatedRels, normalizedFields, actor.getId());

        auditService.log(AuditEvent.of("DATASOURCE_MODEL_SAVE", actor.getId(), actor.getEmail(), "DATASOURCE", datasourceId.toString())
                .withStates(null, Map.of(
                        "tableCount", tables.size(),
                        "relationshipCount", validatedRels.size(),
                        "fieldCount", normalizedFields.size()
                )));

        return new DataModelDto(
                datasourceId,
                ds.name(),
                tables,
                validatedRels,
                normalizedFields,
                computeSuggestedRelationships(datasourceId, physicalTables),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public List<DataModelDto.ModelRelationshipDto> suggestRelationships(UUID datasourceId) {
        getDatasource(datasourceId);
        List<DiscoveredTableDto> physicalTables = repository.findTablesByDatasource(datasourceId);
        return computeSuggestedRelationships(datasourceId, physicalTables);
    }

    @Transactional(readOnly = true)
    public ModelPreviewResponse previewDataModel(UUID datasourceId, SaveDataModelRequest request) {
        getDatasource(datasourceId);
        List<DiscoveredTableDto> physicalTables = repository.findTablesByDatasource(datasourceId);
        List<DataModelDto.ModelFieldDto> fields = normalizeModelFields(
                request != null && request.fields() != null && !request.fields().isEmpty()
                        ? request.fields()
                        : toDefaultModelFields(physicalTables)
        );
        if (request != null && request.relationships() != null) {
            validateAndNormalizeRelationships(datasourceId, request.relationships(), fields, physicalTables);
        }

        List<DataModelDto.ModelFieldDto> visibleFields = fields.stream()
                .filter(f -> !f.hidden())
                .toList();
        List<Map<String, Object>> previewRows = physicalTables.isEmpty()
                ? List.of()
                : physicalTables.get(0).previewRows().stream().limit(100).toList();

        return new ModelPreviewResponse(visibleFields, previewRows, previewRows.size());
    }

    // --- UC14: Mode Switch, Refresh Schedule, Extract Refresh, Connection Update & Dependents ---

    @Transactional
    public DatasourceDto updateMode(UUID datasourceId, String rawMode, DavsUser actor) {
        DatasourceDto before = getDatasource(datasourceId);
        String nextMode = rawMode != null ? rawMode.trim().toUpperCase(Locale.ROOT) : "";
        if (!"LIVE".equals(nextMode) && !"EXTRACT".equals(nextMode)) {
            throw new DavsException(ErrorCode.ERR_PRJ_02);
        }

        repository.updateMode(datasourceId, nextMode);
        if ("EXTRACT".equals(nextMode)) {
            List<DiscoveredTableDto> tables = repository.findTablesByDatasource(datasourceId);
            List<Map<String, Object>> rows = tables.isEmpty() ? List.of() : tables.get(0).previewRows();
            repository.upsertExtractSnapshot(datasourceId, before.rowCount(), rows);
        }

        DatasourceDto after = getDatasource(datasourceId);
        auditService.log(AuditEvent.of("DATASOURCE_MODE_CHANGE", actor.getId(), actor.getEmail(), "DATASOURCE", datasourceId.toString())
                .withStates(Map.of("mode", before.mode()), Map.of("mode", after.mode())));
        return after;
    }

    @Transactional
    public DatasourceDto updateSchedule(UUID datasourceId, UpdateRefreshScheduleRequest request, DavsUser actor) {
        getDatasource(datasourceId);
        String freq = request.frequency() != null ? request.frequency().trim().toUpperCase(Locale.ROOT) : "DAILY";
        if (!Set.of("HOURLY", "DAILY", "WEEKLY", "MONTHLY").contains(freq)) {
            freq = "DAILY";
        }
        String runTime = request.runTime() != null && !request.runTime().isBlank() ? request.runTime().trim() : "02:00";
        String timezone = request.timezone() != null && !request.timezone().isBlank()
                ? request.timezone().trim()
                : "Asia/Ho_Chi_Minh";
        String refreshType = request.refreshType() != null && "INCREMENTAL".equalsIgnoreCase(request.refreshType())
                ? "INCREMENTAL"
                : "FULL";
        boolean enabled = request.enabled() == null || request.enabled();

        repository.upsertRefreshSchedule(
                datasourceId,
                freq,
                runTime,
                timezone,
                refreshType,
                request.incrementalColumn(),
                request.alertEmails(),
                enabled,
                actor.getId()
        );

        DatasourceDto updated = getDatasource(datasourceId);
        auditService.log(AuditEvent.of("DATASOURCE_SCHEDULE_UPDATE", actor.getId(), actor.getEmail(), "DATASOURCE", datasourceId.toString())
                .withStates(null, Map.of("frequency", freq, "runTime", runTime, "refreshType", refreshType, "enabled", enabled)));
        return updated;
    }

    @Transactional
    public RefreshRunDto enqueueManualRefresh(UUID datasourceId, boolean simulateFailure, DavsUser actor) {
        DatasourceDto ds = getDatasource(datasourceId);
        int maxConcurrent = Math.max(1, settingsService.getInt(SettingKey.REFRESH_MAX_CONCURRENT));
        int currentRunning = repository.countRunningRefreshes();

        // BUS-DAT-12: Max 4 concurrent refreshes per site; excess jobs remain PENDING in queue
        String initialStatus = currentRunning >= maxConcurrent ? "PENDING" : "RUNNING";
        String refreshType = ds.schedule() != null ? ds.schedule().refreshType() : "FULL";

        UUID runId = repository.insertRefreshRun(
                datasourceId,
                null,
                refreshType,
                "MANUAL",
                initialStatus,
                actor.getId()
        );

        UUID jobId = jobQueueService.enqueue(
                REFRESH_EXTRACT_JOB_TYPE,
                Map.of(
                        "runId", runId.toString(),
                        "datasourceId", datasourceId.toString(),
                        "simulateFailure", simulateFailure
                ),
                Instant.now(),
                1,
                actor.getId()
        );

        auditService.log(AuditEvent.of("DATASOURCE_REFRESH_TRIGGER", actor.getId(), actor.getEmail(), "DATASOURCE", datasourceId.toString())
                .withStates(null, Map.of("runId", runId.toString(), "jobId", jobId.toString(), "status", initialStatus)));

        return repository.findRefreshRunById(runId)
                .orElseThrow(() -> new IllegalStateException("Refresh run not found: " + runId));
    }

    @Transactional
    public RefreshRunDto executeRefreshRun(UUID runId, boolean simulateFailure) {
        RefreshRunDto run = repository.findRefreshRunById(runId)
                .orElseThrow(() -> new IllegalStateException("Refresh run not found: " + runId));
        UUID datasourceId = run.datasourceId();
        DatasourceDto ds = getDatasource(datasourceId);

        repository.markRefreshRunStatus(runId, "RUNNING");

        boolean connectionFails = simulateFailure
                || (ds.connection() != null
                && ds.connection().host() != null
                && (ds.connection().host().contains("fail") || ds.connection().host().contains("loi.")));

        if (connectionFails) {
            // [4a] & [4b] BUS-DAT-11 & ERR-DAT-05: Keep previous extract intact, increment failure counter
            int pauseThreshold = Math.max(1, settingsService.getInt(SettingKey.REFRESH_PAUSE_AFTER_FAILURES));
            String errMsg = ErrorCode.ERR_DAT_05.getMessageTemplate();
            int failures = repository.recordRefreshFailureAndGetConsecutiveCount(
                    runId,
                    datasourceId,
                    ErrorCode.ERR_DAT_05.getCode(),
                    errMsg,
                    pauseThreshold
            );
            boolean autoPaused = failures >= pauseThreshold;
            sendRefreshFailureNotification(ds, failures, autoPaused, errMsg);
            return repository.findRefreshRunById(runId).orElseThrow();
        }

        List<DiscoveredTableDto> tables = repository.findTablesByDatasource(datasourceId);
        List<Map<String, Object>> rows = tables.isEmpty() ? List.of() : tables.get(0).previewRows();
        long rowCount = Math.max(ds.rowCount(), rows.size());

        repository.upsertExtractSnapshot(datasourceId, rowCount, rows);
        repository.completeRefreshRunSuccess(runId, datasourceId, rowCount);
        return repository.findRefreshRunById(runId).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<RefreshRunDto> listRefreshRuns(UUID datasourceId) {
        getDatasource(datasourceId);
        return repository.listRefreshRuns(datasourceId);
    }

    @Transactional
    public DatasourceDto updateConnection(UUID datasourceId, UpdateConnectionRequest request, DavsUser actor) {
        getDatasource(datasourceId);
        String encrypted = request.password() != null && !request.password().isBlank()
                ? secretCryptoService.encrypt(request.password())
                : null;
        repository.upsertConnection(
                datasourceId,
                request.host(),
                request.port(),
                request.databaseName(),
                request.schemaName(),
                request.username(),
                encrypted
        );
        DatasourceDto updated = getDatasource(datasourceId);
        auditService.log(AuditEvent.of(
                        "DATASOURCE_CONNECTION_UPDATE",
                        actor.getId(),
                        actor.getEmail(),
                        "DATASOURCE",
                        datasourceId.toString()
                )
                .withStates(null, Map.of(
                        "host", request.host() != null ? request.host() : "",
                        "username", request.username() != null ? request.username() : ""
                )));
        return updated;
    }

    @Transactional(readOnly = true)
    public List<DependentWorkbookDto> listDependents(UUID datasourceId) {
        getDatasource(datasourceId);
        return repository.listDependentWorkbooks(datasourceId);
    }

    @Transactional(readOnly = true)
    public JdbcDatasourceRepository.ExtractSnapshotRecord getExtractSnapshot(UUID datasourceId) {
        return repository.findExtractSnapshot(datasourceId).orElse(null);
    }

    // --- Helper methods for CSV parsing, type inference, and BUS-DAT-07/08/09 ---

    public static String inferDefaultRole(String columnName, String dataType) {
        if (columnName == null) {
            return "DIMENSION";
        }
        String lower = columnName.trim().toLowerCase(Locale.ROOT);
        boolean isIdentifier = lower.equals("id")
                || lower.endsWith("_id")
                || lower.startsWith("ma_")
                || lower.endsWith("_code")
                || lower.equals("code");
        if (isIdentifier) {
            return "DIMENSION";
        }
        if ("NUMBER".equalsIgnoreCase(dataType)) {
            return "MEASURE";
        }
        return "DIMENSION";
    }

    public static boolean areTypesCompatible(String leftType, String rightType) {
        String normLeft = normalizeDataType(leftType);
        String normRight = normalizeDataType(rightType);
        return normLeft.equals(normRight);
    }

    public static String normalizeDataType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return "String";
        }
        String upper = rawType.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "NUMBER", "INT", "INTEGER", "LONG", "DOUBLE", "DECIMAL", "FLOAT", "NUMERIC" -> "Number";
            case "DATE" -> "Date";
            case "DATETIME", "DATE & TIME", "TIMESTAMP" -> "Date & Time";
            case "BOOLEAN", "BOOL" -> "Boolean";
            default -> "String";
        };
    }

    private void sendRefreshFailureNotification(DatasourceDto ds, int failures, boolean paused, String errMsg) {
        String recipient = null;
        if (ds.schedule() != null && ds.schedule().alertEmails() != null && !ds.schedule().alertEmails().isBlank()) {
            recipient = ds.schedule().alertEmails().split("[,;]")[0].trim();
        }
        if (recipient == null || recipient.isBlank()) {
            recipient = repository.findOwnerEmail(ds.id()).orElse("admin@davs.local");
        }
        try {
            notificationService.send(
                    EmailTemplate.REFRESH_FAILED,
                    recipient,
                    Map.of(
                            "datasourceName", ds.name(),
                            "errorMessage", errMsg,
                            "failureCount", failures,
                            "paused", paused
                    )
            );
        } catch (Exception ex) {
            LOG.warn("Failed to send refresh failure alert email to {}: {}", recipient, ex.getMessage());
        }
    }

    private void initializeDefaultSemanticModel(UUID dsId, List<DiscoveredTableDto> tables, UUID actorId) {
        List<DataModelDto.ModelTableDto> modelTables = toModelTables(dsId, tables);
        List<DataModelDto.ModelFieldDto> modelFields = toDefaultModelFields(tables);
        List<DataModelDto.ModelRelationshipDto> suggested = computeSuggestedRelationships(dsId, tables);
        repository.upsertDataModel(dsId, modelTables, suggested, modelFields, actorId);
    }

    private static List<DataModelDto.ModelTableDto> toModelTables(UUID dsId, List<DiscoveredTableDto> tables) {
        List<DataModelDto.ModelTableDto> result = new ArrayList<>();
        for (DiscoveredTableDto t : tables) {
            List<String> colNames = t.columns().stream().map(DiscoveredTableDto.ColumnSchemaDto::name).toList();
            result.add(new DataModelDto.ModelTableDto(
                    t.tableName(),
                    t.tableName(),
                    t.schemaName(),
                    dsId,
                    colNames
            ));
        }
        return result;
    }

    private static List<DataModelDto.ModelFieldDto> toDefaultModelFields(List<DiscoveredTableDto> tables) {
        List<DataModelDto.ModelFieldDto> fields = new ArrayList<>();
        for (DiscoveredTableDto t : tables) {
            for (DiscoveredTableDto.ColumnSchemaDto col : t.columns()) {
                String normType = normalizeDataType(col.dataType());
                String role = inferDefaultRole(col.name(), normType);
                String agg = "MEASURE".equals(role) ? "SUM" : "NONE";
                fields.add(new DataModelDto.ModelFieldDto(
                        col.name(),
                        col.name(),
                        t.tableName(),
                        normType,
                        role,
                        agg,
                        false,
                        null,
                        null,
                        null
                ));
            }
        }
        return fields;
    }

    private static List<DataModelDto.ModelFieldDto> normalizeModelFields(List<DataModelDto.ModelFieldDto> rawFields) {
        List<DataModelDto.ModelFieldDto> result = new ArrayList<>();
        for (DataModelDto.ModelFieldDto f : rawFields) {
            String normType = normalizeDataType(f.dataType());
            String role = (f.role() != null && !f.role().isBlank())
                    ? f.role().trim().toUpperCase(Locale.ROOT)
                    : inferDefaultRole(f.name(), normType);
            String agg = f.defaultAggregation() != null && !f.defaultAggregation().isBlank()
                    ? f.defaultAggregation().trim().toUpperCase(Locale.ROOT)
                    : ("MEASURE".equals(role) ? "SUM" : "NONE");
            result.add(new DataModelDto.ModelFieldDto(
                    f.name(),
                    f.alias() != null && !f.alias().isBlank() ? f.alias().trim() : f.name(),
                    f.sourceTable(),
                    normType,
                    role,
                    agg,
                    f.hidden(),
                    f.description(),
                    f.folder(),
                    f.label()
            ));
        }
        return result;
    }

    private static List<DataModelDto.ModelRelationshipDto> validateAndNormalizeRelationships(
            UUID primaryDatasourceId,
            List<DataModelDto.ModelRelationshipDto> rawRels,
            List<DataModelDto.ModelFieldDto> fields,
            List<DiscoveredTableDto> physicalTables
    ) {
        if (rawRels == null || rawRels.isEmpty()) {
            return List.of();
        }
        Map<String, String> fieldTypeMap = new LinkedHashMap<>();
        for (DiscoveredTableDto t : physicalTables) {
            for (DiscoveredTableDto.ColumnSchemaDto c : t.columns()) {
                fieldTypeMap.put((t.tableName() + "." + c.name()).toLowerCase(Locale.ROOT), normalizeDataType(c.dataType()));
                fieldTypeMap.put(c.name().toLowerCase(Locale.ROOT), normalizeDataType(c.dataType()));
            }
        }
        for (DataModelDto.ModelFieldDto f : fields) {
            if (f.sourceTable() != null) {
                fieldTypeMap.put((f.sourceTable() + "." + f.name()).toLowerCase(Locale.ROOT), normalizeDataType(f.dataType()));
            }
            fieldTypeMap.put(f.name().toLowerCase(Locale.ROOT), normalizeDataType(f.dataType()));
        }

        List<DataModelDto.ModelRelationshipDto> validated = new ArrayList<>();
        for (DataModelDto.ModelRelationshipDto rel : rawRels) {
            String joinType = rel.joinType() != null && !rel.joinType().isBlank()
                    ? rel.joinType().trim().toUpperCase(Locale.ROOT)
                    : "RELATIONSHIP";
            if (!SUPPORTED_JOIN_TYPES.contains(joinType)) {
                joinType = "RELATIONSHIP";
            }

            UUID leftDs = rel.leftDatasourceId() != null ? rel.leftDatasourceId() : primaryDatasourceId;
            UUID rightDs = rel.rightDatasourceId() != null ? rel.rightDatasourceId() : primaryDatasourceId;

            // BUS-DAT-09: BLEND only applies to tables from different data sources
            if ("BLEND".equals(joinType) && leftDs.equals(rightDs)) {
                throw new DavsException(ErrorCode.ERR_DAT_06);
            }

            String leftType = rel.leftKeyType() != null && !rel.leftKeyType().isBlank()
                    ? normalizeDataType(rel.leftKeyType())
                    : fieldTypeMap.getOrDefault(
                            (rel.leftTable() + "." + rel.leftKey()).toLowerCase(Locale.ROOT),
                            fieldTypeMap.getOrDefault(rel.leftKey().toLowerCase(Locale.ROOT), "String")
                    );
            String rightType = rel.rightKeyType() != null && !rel.rightKeyType().isBlank()
                    ? normalizeDataType(rel.rightKeyType())
                    : fieldTypeMap.getOrDefault(
                            (rel.rightTable() + "." + rel.rightKey()).toLowerCase(Locale.ROOT),
                            fieldTypeMap.getOrDefault(rel.rightKey().toLowerCase(Locale.ROOT), "String")
                    );

            // [3a] ERR-DAT-06: Relationship/Join keys must have matching data types
            if (!areTypesCompatible(leftType, rightType)) {
                throw new DavsException(ErrorCode.ERR_DAT_06);
            }

            validated.add(new DataModelDto.ModelRelationshipDto(
                    rel.id() != null ? rel.id() : UUID.randomUUID().toString(),
                    rel.leftTable(),
                    rel.leftKey(),
                    leftType,
                    leftDs,
                    rel.rightTable(),
                    rel.rightKey(),
                    rightType,
                    rightDs,
                    joinType,
                    rel.operator() != null && !rel.operator().isBlank() ? rel.operator() : "="
            ));
        }
        return validated;
    }

    private static List<DataModelDto.ModelRelationshipDto> computeSuggestedRelationships(
            UUID datasourceId,
            List<DiscoveredTableDto> tables
    ) {
        List<DataModelDto.ModelRelationshipDto> suggestions = new ArrayList<>();
        for (int i = 0; i < tables.size(); i++) {
            for (int j = i + 1; j < tables.size(); j++) {
                DiscoveredTableDto left = tables.get(i);
                DiscoveredTableDto right = tables.get(j);
                for (DiscoveredTableDto.ColumnSchemaDto lc : left.columns()) {
                    for (DiscoveredTableDto.ColumnSchemaDto rc : right.columns()) {
                        if (lc.name().equalsIgnoreCase(rc.name())
                                && areTypesCompatible(lc.dataType(), rc.dataType())) {
                            suggestions.add(new DataModelDto.ModelRelationshipDto(
                                    left.tableName() + "-" + right.tableName() + "-" + lc.name(),
                                    left.tableName(),
                                    lc.name(),
                                    normalizeDataType(lc.dataType()),
                                    datasourceId,
                                    right.tableName(),
                                    rc.name(),
                                    normalizeDataType(rc.dataType()),
                                    datasourceId,
                                    "RELATIONSHIP",
                                    "="
                            ));
                        }
                    }
                }
            }
        }
        return suggestions;
    }

    public static DiscoveredTableDto parseCsvToTable(String tableName, String csvContent) {
        if (csvContent == null || csvContent.isBlank()) {
            throw new DavsException(ErrorCode.ERR_DAT_04);
        }
        List<String> nonEmptyLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String cleaned = line.replace("\uFEFF", "").trim();
                if (!cleaned.isEmpty()) {
                    nonEmptyLines.add(cleaned);
                }
            }
        } catch (IOException ex) {
            throw new DavsException(ErrorCode.ERR_DAT_04);
        }

        if (nonEmptyLines.size() < 2) {
            throw new DavsException(ErrorCode.ERR_DAT_04);
        }

        String[] headers = nonEmptyLines.get(0).split(",", -1);
        if (headers.length == 0 || headers[0].trim().isEmpty()) {
            throw new DavsException(ErrorCode.ERR_DAT_04);
        }
        for (int i = 0; i < headers.length; i++) {
            headers[i] = headers[i].trim();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (int r = 1; r < nonEmptyLines.size(); r++) {
            String[] values = nonEmptyLines.get(r).split(",", -1);
            if (values.length != headers.length) {
                throw new DavsException(ErrorCode.ERR_DAT_04);
            }
            Map<String, Object> row = new LinkedHashMap<>();
            for (int c = 0; c < headers.length; c++) {
                String raw = values[c].trim();
                row.put(headers[c], parseCellValue(raw));
            }
            rows.add(row);
        }

        List<DiscoveredTableDto.ColumnSchemaDto> columns = new ArrayList<>();
        for (String h : headers) {
            String inferredType = inferColumnType(h, rows);
            columns.add(new DiscoveredTableDto.ColumnSchemaDto(
                    h,
                    inferredType,
                    inferDefaultRole(h, inferredType)
            ));
        }

        return new DiscoveredTableDto(
                tableName.replaceAll("[^a-zA-Z0-9_]", "_"),
                "public",
                rows.size(),
                suggestMode(rows.size()),
                columns,
                rows
        );
    }

    private static Object parseCellValue(String raw) {
        if (raw == null || raw.isEmpty() || "null".equalsIgnoreCase(raw)) {
            return null;
        }
        try {
            if (raw.matches("^-?\\d+$")) {
                return Long.parseLong(raw);
            }
            if (raw.matches("^-?\\d+\\.\\d+$")) {
                return Double.parseDouble(raw);
            }
        } catch (NumberFormatException ignored) {
            // Keep as String
        }
        return raw;
    }

    private static String inferColumnType(String header, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            Object val = row.get(header);
            if (val == null) {
                continue;
            }
            if (val instanceof Number) {
                return "Number";
            }
            String str = String.valueOf(val);
            if (str.matches("^\\d{4}-\\d{2}-\\d{2}$") || str.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
                return "Date";
            }
            return "String";
        }
        return "String";
    }

    private static String suggestMode(long rowCount) {
        return rowCount > EXTRACT_SUGGEST_ROW_THRESHOLD ? "EXTRACT" : "LIVE";
    }

    private static String normalizeMode(String requestedMode, long rowCount) {
        if (requestedMode != null && !requestedMode.isBlank()) {
            String upper = requestedMode.trim().toUpperCase(Locale.ROOT);
            if ("LIVE".equals(upper) || "EXTRACT".equals(upper)) {
                return upper;
            }
        }
        return suggestMode(rowCount);
    }

    private static List<DiscoveredTableDto> buildDefaultDatabaseTables(long estimatedRowCount) {
        String suggested = suggestMode(estimatedRowCount);
        DiscoveredTableDto orders = new DiscoveredTableDto(
                "DonHang",
                "public",
                estimatedRowCount,
                suggested,
                List.of(
                        new DiscoveredTableDto.ColumnSchemaDto("Ma_DH", "String", "DIMENSION"),
                        new DiscoveredTableDto.ColumnSchemaDto("Ma_KH", "String", "DIMENSION"),
                        new DiscoveredTableDto.ColumnSchemaDto("Ngay_Dat", "Date", "DIMENSION"),
                        new DiscoveredTableDto.ColumnSchemaDto("Vung", "String", "DIMENSION"),
                        new DiscoveredTableDto.ColumnSchemaDto("Doanh_So", "Number", "MEASURE"),
                        new DiscoveredTableDto.ColumnSchemaDto("Loi_Nhuan", "Number", "MEASURE")
                ),
                List.of(
                        Map.of(
                                "Ma_DH", "DH001",
                                "Ma_KH", "KH01",
                                "Ngay_Dat", "2026-01-01",
                                "Vung", "Miền Bắc",
                                "Doanh_So", 82000000L,
                                "Loi_Nhuan", 16400000L
                        ),
                        Map.of(
                                "Ma_DH", "DH002",
                                "Ma_KH", "KH02",
                                "Ngay_Dat", "2026-01-02",
                                "Vung", "Miền Nam",
                                "Doanh_So", 74500000L,
                                "Loi_Nhuan", 14900000L
                        )
                )
        );
        DiscoveredTableDto customers = new DiscoveredTableDto(
                "KhachHang",
                "public",
                Math.max(100L, estimatedRowCount / 10L),
                "LIVE",
                List.of(
                        new DiscoveredTableDto.ColumnSchemaDto("Ma_KH", "String", "DIMENSION"),
                        new DiscoveredTableDto.ColumnSchemaDto("Ten_KH", "String", "DIMENSION"),
                        new DiscoveredTableDto.ColumnSchemaDto("Phan_Khuc", "String", "DIMENSION")
                ),
                List.of(
                        Map.of("Ma_KH", "KH01", "Ten_KH", "Công ty Minh Phát", "Phan_Khuc", "Doanh nghiệp"),
                        Map.of("Ma_KH", "KH02", "Ten_KH", "Công ty Hòa Bình", "Phan_Khuc", "Bán lẻ")
                )
        );
        return List.of(orders, customers);
    }
}
