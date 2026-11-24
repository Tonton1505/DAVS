package vn.edu.davs.backend.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import vn.edu.davs.backend.dto.ExportLimitsDto;
import vn.edu.davs.backend.dto.ExportRequest;
import vn.edu.davs.backend.model.DavsUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "UC23 - Report & Data Export", description = "Export dashboard and data to CSV, Excel, PDF, PNG, PowerPoint")
public interface ExportRest {


    @Operation(summary = "Get system export limits (max rows, max size, timeout)")
    @GetMapping("/limits")
    ResponseEntity<ExportLimitsDto> getLimits();


    @Operation(summary = "Export dashboard data or view file")
    @PostMapping
    ResponseEntity<byte[]> exportData(
            @Valid @RequestBody ExportRequest request,
            @AuthenticationPrincipal DavsUser actor
    );
}
