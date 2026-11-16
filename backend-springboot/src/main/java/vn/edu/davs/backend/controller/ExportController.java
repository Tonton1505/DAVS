package vn.edu.davs.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import vn.edu.davs.backend.dto.ExportLimitsDto;
import vn.edu.davs.backend.dto.ExportRequest;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.ExportService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "UC23 - Report & Data Export", description = "Export dashboard and data to CSV, Excel, PDF, PNG, PowerPoint")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @Operation(summary = "Get system export limits (max rows, max size, timeout)")
    @GetMapping({"/exports/limits", "/dashboards/{id}/export/limits"})
    public ResponseEntity<ExportLimitsDto> getLimits() {
        return ResponseEntity.ok(exportService.getLimits());
    }

    @Operation(summary = "Export dashboard data or view file")
    @PostMapping({"/exports", "/dashboards/{id}/export"})
    public ResponseEntity<byte[]> exportData(
            @PathVariable(required = false) UUID id,
            @Valid @RequestBody ExportRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        ExportRequest effectiveReq = request;
        if (request.dashboardId() == null && id != null) {
            effectiveReq = new ExportRequest(
                    id,
                    request.sheetId(),
                    request.format(),
                    request.scope(),
                    request.orientation(),
                    request.pageSize(),
                    request.filters()
            );
        }
        ExportService.ExportResult res = exportService.export(effectiveReq, actor);
        return ResponseEntity.ok()
                .contentType(res.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + res.filename() + "\"")
                .body(res.data());
    }
}
