package vn.edu.davs.backend.controller;

import vn.edu.davs.backend.dto.DashboardDto;
import vn.edu.davs.backend.dto.SaveActionsRequest;
import vn.edu.davs.backend.dto.SaveDashboardRequest;
import vn.edu.davs.backend.service.DashboardActionService;
import vn.edu.davs.backend.service.DashboardService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Dashboards and Interactive Actions (UC17 & UC18).
 */
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardActionService actionService;

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
}
