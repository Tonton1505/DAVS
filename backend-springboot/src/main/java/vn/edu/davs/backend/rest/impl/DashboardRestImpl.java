package vn.edu.davs.backend.rest.impl;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.rest.DashboardRest;

@RequiredArgsConstructor
@RestController
public class DashboardRestImpl implements DashboardRest {


    private final DashboardService dashboardService;

    private final DashboardActionService actionService;


    // =========================================================================
    // UC17 & UC18: Workbook-scoped Dashboard Designer APIs
    // =========================================================================
    @Override
    public ResponseEntity<List<DashboardDto>> listDashboards(@PathVariable UUID workbookId) {
        return ResponseEntity.ok(dashboardService.listDashboards(workbookId));
    }

    @Override
    public ResponseEntity<DashboardDto> getDashboard(@PathVariable UUID workbookId, @PathVariable UUID dashId) {
        return ResponseEntity.ok(dashboardService.getDashboard(dashId));
    }

    @Override
    public ResponseEntity<DashboardDto> createDashboard(
            @PathVariable UUID workbookId,
            @Valid @RequestBody SaveDashboardRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dashboardService.createDashboard(workbookId, request));
    }

    @Override
    public ResponseEntity<DashboardDto> updateDashboard(
            @PathVariable UUID workbookId,
            @PathVariable UUID dashId,
            @Valid @RequestBody SaveDashboardRequest request
    ) {
        return ResponseEntity.ok(dashboardService.updateDashboard(dashId, request));
    }

    @Override
    public ResponseEntity<Void> deleteDashboard(@PathVariable UUID workbookId, @PathVariable UUID dashId) {
        dashboardService.deleteDashboard(dashId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<DashboardDto> saveActions(
            @PathVariable UUID workbookId,
            @PathVariable UUID dashId,
            @Valid @RequestBody SaveActionsRequest request
    ) {
        return ResponseEntity.ok(actionService.saveActions(workbookId, dashId, request));
    }

    @Override
    public ResponseEntity<List<String>> getTrustedDomains() {
        return ResponseEntity.ok(actionService.getTrustedDomains());
    }


    // =========================================================================
    // UC19: Dashboard Viewer, Metadata & Interactive Query APIs
    // =========================================================================
    @Override
    public ResponseEntity<DashboardDto> getDashboardById(@PathVariable UUID id) {
        return ResponseEntity.ok(dashboardService.getById(id));
    }

    @Override
    public ResponseEntity<DashboardViewDto> getDashboardView(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(dashboardService.getDashboardView(id, actor));
    }

    @Override
    public ResponseEntity<DashboardDto> createDashboard(
            @Valid @RequestBody CreateDashboardRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dashboardService.create(request, actor));
    }

    @Override
    public ResponseEntity<QueryResultDto> executeQuery(
            @RequestBody QueryRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(dashboardService.executeQuery(request, actor));
    }
}
