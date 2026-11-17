package vn.edu.davs.backend.controller;

import jakarta.validation.Valid;
import vn.edu.davs.backend.dto.DatasourceRlsStatusDto;
import vn.edu.davs.backend.dto.RowFilterDto;
import vn.edu.davs.backend.dto.SaveRowFilterRequest;
import vn.edu.davs.backend.dto.ToggleDatasourceRlsRequest;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.RowFilterService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin REST controller for Row-Level Security (RLS) policy management (T-101).
 */
@RestController
@RequestMapping("/api/v1/admin/rls")
@RequiredArgsConstructor
public class RowFilterController {

    private final RowFilterService rowFilterService;

    @GetMapping("/datasources")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<List<DatasourceRlsStatusDto>> listDatasourceRlsStatuses(
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(rowFilterService.listDatasourceRlsStatuses(actor));
    }

    @PutMapping("/datasources/{datasourceId}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<DatasourceRlsStatusDto> toggleDatasourceRls(
            @PathVariable UUID datasourceId,
            @RequestBody ToggleDatasourceRlsRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(rowFilterService.toggleDatasourceRls(datasourceId, request.enabled(), actor));
    }

    @GetMapping("/policies")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<List<RowFilterDto>> listRowFilters(
            @RequestParam(value = "datasourceId", required = false) UUID datasourceId,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(rowFilterService.listRowFilters(datasourceId, actor));
    }

    @PostMapping("/policies")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<RowFilterDto> saveRowFilter(
            @Valid @RequestBody SaveRowFilterRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rowFilterService.saveRowFilter(request, actor));
    }

    @DeleteMapping("/policies/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<Void> deleteRowFilter(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        rowFilterService.deleteRowFilter(id, actor);
        return ResponseEntity.noContent().build();
    }
}
