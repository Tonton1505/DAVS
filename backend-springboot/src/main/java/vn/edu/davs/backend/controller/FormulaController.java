package vn.edu.davs.backend.controller;

import vn.edu.davs.backend.dto.CalculatedFieldDto;
import vn.edu.davs.backend.dto.FormulaValidateRequest;
import vn.edu.davs.backend.dto.FormulaValidateResponse;
import vn.edu.davs.backend.dto.SaveCalculatedFieldRequest;
import vn.edu.davs.backend.service.FormulaService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Formula validation and Calculated Fields (UC16).
 */
@RestController
@RequiredArgsConstructor
public class FormulaController {

    private final FormulaService formulaService;

    @PostMapping("/api/v1/formulas/validate")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    public ResponseEntity<FormulaValidateResponse> validateFormula(
            @Valid @RequestBody FormulaValidateRequest request
    ) {
        return ResponseEntity.ok(formulaService.validateFormula(request.datasourceId(), request.formula(), null));
    }

    @GetMapping("/api/v1/datasources/{datasourceId}/calculated-fields")
    public ResponseEntity<List<CalculatedFieldDto>> listCalculatedFields(@PathVariable UUID datasourceId) {
        return ResponseEntity.ok(formulaService.listCalculatedFields(datasourceId));
    }

    @PostMapping("/api/v1/datasources/{datasourceId}/calculated-fields")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    public ResponseEntity<CalculatedFieldDto> createCalculatedField(
            @PathVariable UUID datasourceId,
            @Valid @RequestBody SaveCalculatedFieldRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(formulaService.createCalculatedField(datasourceId, request));
    }

    @PutMapping("/api/v1/datasources/{datasourceId}/calculated-fields/{fieldId}")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    public ResponseEntity<CalculatedFieldDto> updateCalculatedField(
            @PathVariable UUID datasourceId,
            @PathVariable UUID fieldId,
            @Valid @RequestBody SaveCalculatedFieldRequest request
    ) {
        return ResponseEntity.ok(formulaService.updateCalculatedField(fieldId, request));
    }

    @DeleteMapping("/api/v1/datasources/{datasourceId}/calculated-fields/{fieldId}")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    public ResponseEntity<Void> deleteCalculatedField(
            @PathVariable UUID datasourceId,
            @PathVariable UUID fieldId
    ) {
        formulaService.deleteCalculatedField(fieldId);
        return ResponseEntity.noContent().build();
    }
}
