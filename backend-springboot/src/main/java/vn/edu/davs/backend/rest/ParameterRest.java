package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.ParameterDto;
import vn.edu.davs.backend.dto.SaveParameterRequest;
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
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for Workbook Parameters (UC16).
 */
@RequestMapping("/api/v1/workbooks/{workbookId}/parameters")
public interface ParameterRest {


    @GetMapping
    ResponseEntity<List<ParameterDto>> listParameters(@PathVariable UUID workbookId);


    @PostMapping
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    ResponseEntity<ParameterDto> createParameter(
            @PathVariable UUID workbookId,
            @Valid @RequestBody SaveParameterRequest request
    );


    @PutMapping("/{paramId}")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    ResponseEntity<ParameterDto> updateParameter(
            @PathVariable UUID workbookId,
            @PathVariable UUID paramId,
            @Valid @RequestBody SaveParameterRequest request
    );


    @DeleteMapping("/{paramId}")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    ResponseEntity<Void> deleteParameter(
            @PathVariable UUID workbookId,
            @PathVariable UUID paramId
    );
}
