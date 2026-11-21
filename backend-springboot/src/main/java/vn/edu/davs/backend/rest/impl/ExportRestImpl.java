package vn.edu.davs.backend.rest.impl;

import jakarta.validation.Valid;
import vn.edu.davs.backend.dto.ExportLimitsDto;
import vn.edu.davs.backend.dto.ExportRequest;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.rest.ExportRest;

@RequiredArgsConstructor
@RestController
public class ExportRestImpl implements ExportRest {


    private final ExportService exportService;

    @Override
    public ResponseEntity<ExportLimitsDto> getLimits() {
        return ResponseEntity.ok(exportService.getLimits());
    }

    @Override
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
