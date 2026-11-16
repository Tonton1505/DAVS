package vn.edu.davs.backend.controller;

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
import vn.edu.davs.backend.dto.UpdateDatasourceModeRequest;
import vn.edu.davs.backend.dto.UpdateRefreshScheduleRequest;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.DatasourceAndModelService;
import vn.edu.davs.backend.service.PermissionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST endpoints for UC11 (Connectors &amp; Data Source Connections),
 * UC13 (Semantic Data Model), and UC14 (Extract Management &amp; Refresh).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DatasourceController {

    private final DatasourceAndModelService datasourceService;
    private final PermissionService permissionService;

    @GetMapping("/connectors")
    public ResponseEntity<List<ConnectorDto>> listConnectors(@AuthenticationPrincipal DavsUser actor) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(datasourceService.listConnectors());
    }

    @PostMapping("/datasources/test")
    public ResponseEntity<TestConnectionResponse> testConnection(
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody TestConnectionRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(datasourceService.testConnection(request));
    }

    @PostMapping(value = "/datasources", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DatasourceDto> createDatasourceJson(
            @AuthenticationPrincipal DavsUser actor,
            @RequestBody CreateDatasourceRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        DatasourceDto created = datasourceService.createDatasource(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(value = "/datasources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DatasourceDto> createDatasourceMultipart(
            @AuthenticationPrincipal DavsUser actor,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "connectorType", required = false) String connectorType,
            @RequestParam(value = "mode", required = false) String mode,
            @RequestParam(value = "projectId", required = false) UUID projectId
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        DatasourceDto created = datasourceService.createDatasourceFromFile(
                file, name, connectorType, mode, projectId, actor
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/datasources")
    public ResponseEntity<List<DatasourceDto>> listDatasources(
            @AuthenticationPrincipal DavsUser actor,
            @RequestParam(value = "managed", required = false) Boolean managed
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(datasourceService.listDatasources());
    }

    @GetMapping("/datasources/{id}")
    public ResponseEntity<DatasourceDto> getDatasource(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(datasourceService.getDatasource(id));
    }

    @GetMapping("/datasources/{id}/tables")
    public ResponseEntity<List<DiscoveredTableDto>> listTables(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(datasourceService.listTables(id));
    }

    // --- UC13: Semantic Data Model ---

    @GetMapping("/datasources/{id}/model")
    public ResponseEntity<DataModelDto> getDataModel(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(datasourceService.getDataModel(id));
    }

    @PutMapping("/datasources/{id}/model")
    public ResponseEntity<DataModelDto> saveDataModel(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestBody SaveDataModelRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(datasourceService.saveDataModel(id, request, actor));
    }

    @PostMapping("/datasources/{id}/model/suggest-relationships")
    public ResponseEntity<List<DataModelDto.ModelRelationshipDto>> suggestRelationships(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(datasourceService.suggestRelationships(id));
    }

    @PostMapping("/datasources/{id}/model/preview")
    public ResponseEntity<ModelPreviewResponse> previewModel(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestBody(required = false) SaveDataModelRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(datasourceService.previewDataModel(id, request));
    }

    // --- UC14: Extract Management & Refresh ---

    @PutMapping("/datasources/{id}/mode")
    public ResponseEntity<DatasourceDto> updateMode(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDatasourceModeRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(datasourceService.updateMode(id, request.mode(), actor));
    }

    @PutMapping("/datasources/{id}/refresh-schedule")
    public ResponseEntity<DatasourceDto> updateRefreshSchedule(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestBody UpdateRefreshScheduleRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(datasourceService.updateSchedule(id, request, actor));
    }

    @PostMapping("/datasources/{id}/refresh")
    public ResponseEntity<RefreshRunDto> triggerRefresh(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestParam(value = "simulateFailure", defaultValue = "false") boolean simulateFailure
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        RefreshRunDto run = datasourceService.enqueueManualRefresh(id, simulateFailure, actor);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(run);
    }

    @GetMapping("/datasources/{id}/refresh-runs")
    public ResponseEntity<List<RefreshRunDto>> listRefreshRuns(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(datasourceService.listRefreshRuns(id));
    }

    @PutMapping("/datasources/{id}/connection")
    public ResponseEntity<DatasourceDto> updateConnection(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestBody UpdateConnectionRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(datasourceService.updateConnection(id, request, actor));
    }

    @GetMapping("/datasources/{id}/dependents")
    public ResponseEntity<List<DependentWorkbookDto>> listDependents(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(datasourceService.listDependents(id));
    }
}
