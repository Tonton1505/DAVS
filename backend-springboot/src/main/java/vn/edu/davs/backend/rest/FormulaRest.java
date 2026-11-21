package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.CalculatedFieldDto;
import vn.edu.davs.backend.dto.FormulaValidateRequest;
import vn.edu.davs.backend.dto.FormulaValidateResponse;
import vn.edu.davs.backend.dto.SaveCalculatedFieldRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Controller for Formula validation and Calculated Fields (UC16).
 */
public interface FormulaRest {


    @PostMapping("/api/v1/formulas/validate")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    ResponseEntity<FormulaValidateResponse> validateFormula(
            @Valid @RequestBody FormulaValidateRequest request
    );


    @GetMapping("/api/v1/datasources/{datasourceId}/calculated-fields")
    ResponseEntity<List<CalculatedFieldDto>> listCalculatedFields(@PathVariable UUID datasourceId);


    @PostMapping("/api/v1/datasources/{datasourceId}/calculated-fields")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    ResponseEntity<CalculatedFieldDto> createCalculatedField(
            @PathVariable UUID datasourceId,
            @Valid @RequestBody SaveCalculatedFieldRequest request
    );


    @PutMapping("/api/v1/datasources/{datasourceId}/calculated-fields/{fieldId}")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    ResponseEntity<CalculatedFieldDto> updateCalculatedField(
            @PathVariable UUID datasourceId,
            @PathVariable UUID fieldId,
            @Valid @RequestBody SaveCalculatedFieldRequest request
    );


    @DeleteMapping("/api/v1/datasources/{datasourceId}/calculated-fields/{fieldId}")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    ResponseEntity<Void> deleteCalculatedField(
            @PathVariable UUID datasourceId,
            @PathVariable UUID fieldId
    );
}
