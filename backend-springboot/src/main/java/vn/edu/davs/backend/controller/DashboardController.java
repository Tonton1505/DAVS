package vn.edu.davs.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import vn.edu.davs.backend.dto.CreateDashboardRequest;
import vn.edu.davs.backend.dto.DashboardDto;
import vn.edu.davs.backend.dto.DashboardViewDto;
import vn.edu.davs.backend.dto.QueryRequest;
import vn.edu.davs.backend.dto.QueryResultDto;
import vn.edu.davs.backend.dto.SaveActionsRequest;
import vn.edu.davs.backend.dto.SaveDashboardRequest;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.DashboardActionService;
import vn.edu.davs.backend.service.DashboardService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Dashboards, Interactive Actions (UC17 & UC18), Viewing & Query (UC19).
 */
@Tag(name = "Dashboards", description = "Dashboard authoring, viewing, interaction and actions")
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardActionService actionService;

    // =========================================================================
    // UC17 & UC18: Workbook-scoped Dashboard Designer APIs
    // =========================================================================

    @GetMapping("/api/v1/workbooks/{workbookId}/dashboards")
    public ResponseEntity<List<DashboardDto>> listDashboards(@PathVariable UUID workbookId) {
        return ResponseEntity.ok(dashboardService.listDashboards(workbookId));
    }

    @GetMapping("/api/v1/workbooks/{workbookId}/dashboards/{dashId}")
    public ResponseEntity<DashboardDto> getDashboard(@PathVariable UUID workbookId, @PathVariable UUID dashId) {
        return ResponseEntity.ok(dashboardService.getDashboard(dashId));
    }

    @PostMapping("/api/v1/workbooks/{workbookId}/dashboards")
    public ResponseEntity<DashboardDto> createDashboard(
            @PathVariable UUID workbookId,
            @Valid @RequestBody SaveDashboardRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dashboardService.createDashboard(workbookId, request));
    }

    @PutMapping("/api/v1/workbooks/{workbookId}/dashboards/{dashId}")
    public ResponseEntity<DashboardDto> updateDashboard(
            @PathVariable UUID workbookId,
            @PathVariable UUID dashId,
            @Valid @RequestBody SaveDashboardRequest request
    ) {
        return ResponseEntity.ok(dashboardService.updateDashboard(dashId, request));
    }

    @DeleteMapping("/api/v1/workbooks/{workbookId}/dashboards/{dashId}")
    public ResponseEntity<Void> deleteDashboard(@PathVariable UUID workbookId, @PathVariable UUID dashId) {
        dashboardService.deleteDashboard(dashId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/v1/workbooks/{workbookId}/dashboards/{dashId}/actions")
    public ResponseEntity<DashboardDto> saveActions(
            @PathVariable UUID workbookId,
            @PathVariable UUID dashId,
            @Valid @RequestBody SaveActionsRequest request
    ) {
        return ResponseEntity.ok(actionService.saveActions(workbookId, dashId, request));
    }

    @GetMapping("/api/v1/settings/trusted-domains")
    public ResponseEntity<List<String>> getTrustedDomains() {
        return ResponseEntity.ok(actionService.getTrustedDomains());
    }

    // =========================================================================
    // UC19: Dashboard Viewer, Metadata & Interactive Query APIs
    // =========================================================================

    @Operation(summary = "Get dashboard details")
    @GetMapping("/api/v1/dashboards/{id}")
    public ResponseEntity<DashboardDto> getDashboardById(@PathVariable UUID id) {
        return ResponseEntity.ok(dashboardService.getById(id));
    }

    @Operation(summary = "Get published dashboard view with effective permissions and metadata")
    @GetMapping("/api/v1/dashboards/{id}/view")
    public ResponseEntity<DashboardViewDto> getDashboardView(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(dashboardService.getDashboardView(id, actor));
    }

    @Operation(summary = "Create a dashboard in workbook")
    @PostMapping("/api/v1/dashboards")
    public ResponseEntity<DashboardDto> createDashboard(
            @Valid @RequestBody CreateDashboardRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dashboardService.create(request, actor));
    }

    @Operation(summary = "Execute interactive query on dashboard with applied filters and parameters")
    @PostMapping("/api/v1/dashboards/query")
    public ResponseEntity<QueryResultDto> executeQuery(
            @RequestBody QueryRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(dashboardService.executeQuery(request, actor));
    }
}
