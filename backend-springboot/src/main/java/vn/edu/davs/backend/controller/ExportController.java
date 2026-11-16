package vn.edu.davs.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import vn.edu.davs.backend.dto.ExportLimitsDto;
import vn.edu.davs.backend.dto.ExportRequest;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "UC23 - Report & Data Export", description = "Export dashboard and data to CSV, Excel, PDF, PNG, PowerPoint")
@RestController
@RequestMapping("/api/v1/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @Operation(summary = "Get system export limits (max rows, max size, timeout)")
    @GetMapping("/limits")
    public ResponseEntity<ExportLimitsDto> getLimits() {
        return ResponseEntity.ok(exportService.getLimits());
    }

    @Operation(summary = "Export dashboard data or view file")
    @PostMapping
    public ResponseEntity<byte[]> exportData(
            @Valid @RequestBody ExportRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        ExportService.ExportResult res = exportService.export(request, actor);
        return ResponseEntity.ok()
                .contentType(res.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + res.filename() + "\"")
                .body(res.data());
    }
}
