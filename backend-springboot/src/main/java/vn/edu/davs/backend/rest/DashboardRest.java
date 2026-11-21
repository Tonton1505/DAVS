package vn.edu.davs.backend.rest;

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
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Controller for Dashboards, Interactive Actions (UC17 & UC18), Viewing & Query (UC19).
 */
@Tag(name = "Dashboards", description = "Dashboard authoring, viewing, interaction and actions")
public interface DashboardRest {


    // =========================================================================
    // UC17 & UC18: Workbook-scoped Dashboard Designer APIs
    // =========================================================================

    @GetMapping("/api/v1/workbooks/{workbookId}/dashboards")
    ResponseEntity<List<DashboardDto>> listDashboards(@PathVariable UUID workbookId);


    @GetMapping("/api/v1/workbooks/{workbookId}/dashboards/{dashId}")
    ResponseEntity<DashboardDto> getDashboard(@PathVariable UUID workbookId, @PathVariable UUID dashId);


    @PostMapping("/api/v1/workbooks/{workbookId}/dashboards")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    ResponseEntity<DashboardDto> createDashboard(
            @PathVariable UUID workbookId,
            @Valid @RequestBody SaveDashboardRequest request
    );


    @PutMapping("/api/v1/workbooks/{workbookId}/dashboards/{dashId}")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    ResponseEntity<DashboardDto> updateDashboard(
            @PathVariable UUID workbookId,
            @PathVariable UUID dashId,
            @Valid @RequestBody SaveDashboardRequest request
    );


    @DeleteMapping("/api/v1/workbooks/{workbookId}/dashboards/{dashId}")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    ResponseEntity<Void> deleteDashboard(@PathVariable UUID workbookId, @PathVariable UUID dashId);


    @PutMapping("/api/v1/workbooks/{workbookId}/dashboards/{dashId}/actions")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    ResponseEntity<DashboardDto> saveActions(
            @PathVariable UUID workbookId,
            @PathVariable UUID dashId,
            @Valid @RequestBody SaveActionsRequest request
    );


    @GetMapping("/api/v1/settings/trusted-domains")
    ResponseEntity<List<String>> getTrustedDomains();


    // =========================================================================
    // UC19: Dashboard Viewer, Metadata & Interactive Query APIs
    // =========================================================================

    @Operation(summary = "Get dashboard details")
    @GetMapping("/api/v1/dashboards/{id}")
    ResponseEntity<DashboardDto> getDashboardById(@PathVariable UUID id);


    @Operation(summary = "Get published dashboard view with effective permissions and metadata")
    @GetMapping("/api/v1/dashboards/{id}/view")
    ResponseEntity<DashboardViewDto> getDashboardView(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    );


    @Operation(summary = "Create a dashboard in workbook")
    @PostMapping("/api/v1/dashboards")
    ResponseEntity<DashboardDto> createDashboard(
            @Valid @RequestBody CreateDashboardRequest request,
            @AuthenticationPrincipal DavsUser actor
    );


    @Operation(summary = "Execute interactive query on dashboard with applied filters and parameters")
    @PostMapping("/api/v1/dashboards/query")
    ResponseEntity<QueryResultDto> executeQuery(
            @RequestBody QueryRequest request,
            @AuthenticationPrincipal DavsUser actor
    );
}
