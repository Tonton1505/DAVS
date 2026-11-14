package vn.edu.davs.backend.controller;

import vn.edu.davs.backend.dto.PrepFlowDto;
import vn.edu.davs.backend.dto.PrepPreviewRequest;
import vn.edu.davs.backend.dto.PrepPreviewResponse;
import vn.edu.davs.backend.dto.PrepRunDto;
import vn.edu.davs.backend.dto.SavePrepFlowRequest;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.DataPrepService;
import vn.edu.davs.backend.service.PermissionService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

/**
 * REST endpoints for UC12 Data Preparation &amp; Cleaning ({@code /api/v1/prep-flows}).
 */
@RestController
@RequestMapping("/api/v1/prep-flows")
@RequiredArgsConstructor
public class PrepFlowController {

    private final DataPrepService dataPrepService;
    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<PrepFlowDto>> listFlows(
            @AuthenticationPrincipal DavsUser actor,
            @RequestParam(value = "datasourceId", required = false) UUID datasourceId
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(dataPrepService.listFlows(datasourceId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrepFlowDto> getFlow(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(dataPrepService.getFlow(id));
    }

    @PostMapping
    public ResponseEntity<PrepFlowDto> createFlow(
            @AuthenticationPrincipal DavsUser actor,
            @RequestBody SavePrepFlowRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        PrepFlowDto created = dataPrepService.createFlow(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrepFlowDto> updateFlow(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestBody SavePrepFlowRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(dataPrepService.updateFlow(id, request, actor));
    }

    @PostMapping("/{id}/preview")
    public ResponseEntity<PrepPreviewResponse> previewFlow(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestBody(required = false) PrepPreviewRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(dataPrepService.previewFlow(id, request));
    }

    @PostMapping("/{id}/run")
    public ResponseEntity<PrepRunDto> runFlow(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestParam(value = "simulateFailure", defaultValue = "false") boolean simulateFailure
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        PrepRunDto run = dataPrepService.enqueueRun(id, simulateFailure, actor);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(run);
    }

    @GetMapping("/{id}/runs")
    public ResponseEntity<List<PrepRunDto>> listRuns(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    ) {
        permissionService.requireRole(actor, SiteRole.CREATOR);
        return ResponseEntity.ok(dataPrepService.listRuns(id));
    }
}
