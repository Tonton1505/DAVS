package vn.edu.davs.backend.controller;

import vn.edu.davs.backend.dto.ParameterDto;
import vn.edu.davs.backend.dto.SaveParameterRequest;
import vn.edu.davs.backend.service.ParameterService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Workbook Parameters (UC16).
 */
@RestController
@RequestMapping("/api/v1/workbooks/{workbookId}/parameters")
@RequiredArgsConstructor
public class ParameterController {

    private final ParameterService parameterService;

    @GetMapping
    public ResponseEntity<List<ParameterDto>> listParameters(@PathVariable UUID workbookId) {
        return ResponseEntity.ok(parameterService.listParameters(workbookId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    public ResponseEntity<ParameterDto> createParameter(
            @PathVariable UUID workbookId,
            @Valid @RequestBody SaveParameterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(parameterService.createParameter(workbookId, request));
    }

    @PutMapping("/{paramId}")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    public ResponseEntity<ParameterDto> updateParameter(
            @PathVariable UUID workbookId,
            @PathVariable UUID paramId,
            @Valid @RequestBody SaveParameterRequest request
    ) {
        return ResponseEntity.ok(parameterService.updateParameter(paramId, request));
    }

    @DeleteMapping("/{paramId}")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    public ResponseEntity<Void> deleteParameter(
            @PathVariable UUID workbookId,
            @PathVariable UUID paramId
    ) {
        parameterService.deleteParameter(paramId);
        return ResponseEntity.noContent().build();
    }
}
