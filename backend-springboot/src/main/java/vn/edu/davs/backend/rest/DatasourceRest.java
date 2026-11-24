package vn.edu.davs.backend.rest;

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
import vn.edu.davs.backend.model.DavsUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST endpoints for UC11 (Connectors &amp; Data Source Connections),
 * UC13 (Semantic Data Model), and UC14 (Extract Management &amp; Refresh).
 */
public interface DatasourceRest {


    @GetMapping("/connectors")
    ResponseEntity<List<ConnectorDto>> listConnectors(@AuthenticationPrincipal DavsUser actor);


    @PostMapping("/datasources/test")
    ResponseEntity<TestConnectionResponse> testConnection(
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody TestConnectionRequest request
    );


    @PostMapping(value = "/datasources", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DatasourceDto> createDatasourceJson(
            @AuthenticationPrincipal DavsUser actor,
            @RequestBody CreateDatasourceRequest request
    );


    @PostMapping(value = "/datasources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<DatasourceDto> createDatasourceMultipart(
            @AuthenticationPrincipal DavsUser actor,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "connectorType", required = false) String connectorType,
            @RequestParam(value = "mode", required = false) String mode,
            @RequestParam(value = "projectId", required = false) UUID projectId
    );


    @GetMapping("/datasources")
    ResponseEntity<List<DatasourceDto>> listDatasources(
            @AuthenticationPrincipal DavsUser actor,
            @RequestParam(value = "managed", required = false) Boolean managed
    );


    @GetMapping("/datasources/{id}")
    ResponseEntity<DatasourceDto> getDatasource(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    );


    @GetMapping("/datasources/{id}/tables")
    ResponseEntity<List<DiscoveredTableDto>> listTables(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    );


    // --- UC13: Semantic Data Model ---

    @GetMapping("/datasources/{id}/model")
    ResponseEntity<DataModelDto> getDataModel(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    );


    @PutMapping("/datasources/{id}/model")
    ResponseEntity<DataModelDto> saveDataModel(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestBody SaveDataModelRequest request
    );


    @PostMapping("/datasources/{id}/model/suggest-relationships")
    ResponseEntity<List<DataModelDto.ModelRelationshipDto>> suggestRelationships(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    );


    @PostMapping("/datasources/{id}/model/preview")
    ResponseEntity<ModelPreviewResponse> previewModel(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestBody(required = false) SaveDataModelRequest request
    );


    // --- UC14: Extract Management & Refresh ---

    @PutMapping("/datasources/{id}/mode")
    ResponseEntity<DatasourceDto> updateMode(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDatasourceModeRequest request
    );


    @PutMapping("/datasources/{id}/refresh-schedule")
    ResponseEntity<DatasourceDto> updateRefreshSchedule(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestBody UpdateRefreshScheduleRequest request
    );


    @PostMapping("/datasources/{id}/refresh")
    ResponseEntity<RefreshRunDto> triggerRefresh(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestParam(value = "simulateFailure", defaultValue = "false") boolean simulateFailure
    );


    @GetMapping("/datasources/{id}/refresh-runs")
    ResponseEntity<List<RefreshRunDto>> listRefreshRuns(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    );


    @PutMapping("/datasources/{id}/connection")
    ResponseEntity<DatasourceDto> updateConnection(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestBody UpdateConnectionRequest request
    );


    @GetMapping("/datasources/{id}/dependents")
    ResponseEntity<List<DependentWorkbookDto>> listDependents(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    );
}
