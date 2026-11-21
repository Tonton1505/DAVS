package vn.edu.davs.backend.rest.impl;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.rest.FormulaRest;

@RequiredArgsConstructor
@RestController
public class FormulaRestImpl implements FormulaRest {


    private final FormulaService formulaService;

    @Override
    public ResponseEntity<FormulaValidateResponse> validateFormula(
            @Valid @RequestBody FormulaValidateRequest request
    ) {
        return ResponseEntity.ok(formulaService.validateFormula(request.datasourceId(), request.formula(), null));
    }

    @Override
    public ResponseEntity<List<CalculatedFieldDto>> listCalculatedFields(@PathVariable UUID datasourceId) {
        return ResponseEntity.ok(formulaService.listCalculatedFields(datasourceId));
    }

    @Override
    public ResponseEntity<CalculatedFieldDto> createCalculatedField(
            @PathVariable UUID datasourceId,
            @Valid @RequestBody SaveCalculatedFieldRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(formulaService.createCalculatedField(datasourceId, request));
    }

    @Override
    public ResponseEntity<CalculatedFieldDto> updateCalculatedField(
            @PathVariable UUID datasourceId,
            @PathVariable UUID fieldId,
            @Valid @RequestBody SaveCalculatedFieldRequest request
    ) {
        return ResponseEntity.ok(formulaService.updateCalculatedField(fieldId, request));
    }

    @Override
    public ResponseEntity<Void> deleteCalculatedField(
            @PathVariable UUID datasourceId,
            @PathVariable UUID fieldId
    ) {
        formulaService.deleteCalculatedField(fieldId);
        return ResponseEntity.noContent().build();
    }
}
