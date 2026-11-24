package vn.edu.davs.backend.rest.impl;

import vn.edu.davs.backend.dto.ParameterDto;
import vn.edu.davs.backend.dto.SaveParameterRequest;
import vn.edu.davs.backend.service.ParameterService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.davs.backend.rest.ParameterRest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workbooks/{workbookId}/parameters")
public class ParameterRestImpl implements ParameterRest {


    private final ParameterService parameterService;

    @Override
    public ResponseEntity<List<ParameterDto>> listParameters(@PathVariable UUID workbookId) {
        return ResponseEntity.ok(parameterService.listParameters(workbookId));
    }

    @Override
    public ResponseEntity<ParameterDto> createParameter(
            @PathVariable UUID workbookId,
            @Valid @RequestBody SaveParameterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(parameterService.createParameter(workbookId, request));
    }

    @Override
    public ResponseEntity<ParameterDto> updateParameter(
            @PathVariable UUID workbookId,
            @PathVariable UUID paramId,
            @Valid @RequestBody SaveParameterRequest request
    ) {
        return ResponseEntity.ok(parameterService.updateParameter(paramId, request));
    }

    @Override
    public ResponseEntity<Void> deleteParameter(
            @PathVariable UUID workbookId,
            @PathVariable UUID paramId
    ) {
        parameterService.deleteParameter(paramId);
        return ResponseEntity.noContent().build();
    }
}
