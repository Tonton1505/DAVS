package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.PrepFlowDto;
import vn.edu.davs.backend.dto.PrepPreviewRequest;
import vn.edu.davs.backend.dto.PrepPreviewResponse;
import vn.edu.davs.backend.dto.PrepRunDto;
import vn.edu.davs.backend.dto.SavePrepFlowRequest;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST endpoints for UC12 Data Preparation &amp; Cleaning ({@code /api/v1/prep-flows}).
 */
@RequestMapping("/api/v1/prep-flows")
public interface PrepFlowRest {


    @GetMapping
    ResponseEntity<List<PrepFlowDto>> listFlows(
            @AuthenticationPrincipal DavsUser actor,
            @RequestParam(value = "datasourceId", required = false) UUID datasourceId
    );


    @GetMapping("/{id}")
    ResponseEntity<PrepFlowDto> getFlow(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    );


    @PostMapping
    ResponseEntity<PrepFlowDto> createFlow(
            @AuthenticationPrincipal DavsUser actor,
            @RequestBody SavePrepFlowRequest request
    );


    @PutMapping("/{id}")
    ResponseEntity<PrepFlowDto> updateFlow(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestBody SavePrepFlowRequest request
    );


    @PostMapping("/{id}/preview")
    ResponseEntity<PrepPreviewResponse> previewFlow(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestBody(required = false) PrepPreviewRequest request
    );


    @PostMapping("/{id}/run")
    ResponseEntity<PrepRunDto> runFlow(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestParam(value = "simulateFailure", defaultValue = "false") boolean simulateFailure
    );


    @GetMapping("/{id}/runs")
    ResponseEntity<List<PrepRunDto>> listRuns(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    );
}
