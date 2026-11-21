package vn.edu.davs.backend.rest;

import jakarta.validation.Valid;
import vn.edu.davs.backend.dto.DatasourceRlsStatusDto;
import vn.edu.davs.backend.dto.RowFilterDto;
import vn.edu.davs.backend.dto.SaveRowFilterRequest;
import vn.edu.davs.backend.dto.ToggleDatasourceRlsRequest;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.UUID;
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

/**
 * Admin REST controller for Row-Level Security (RLS) policy management (T-101).
 */
@RequestMapping("/api/v1/admin/rls")
public interface RowFilterRest {


    @GetMapping("/datasources")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<List<DatasourceRlsStatusDto>> listDatasourceRlsStatuses(
            @AuthenticationPrincipal DavsUser actor
    );


    @PutMapping("/datasources/{datasourceId}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<DatasourceRlsStatusDto> toggleDatasourceRls(
            @PathVariable UUID datasourceId,
            @RequestBody ToggleDatasourceRlsRequest request,
            @AuthenticationPrincipal DavsUser actor
    );


    @GetMapping("/policies")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<List<RowFilterDto>> listRowFilters(
            @RequestParam(value = "datasourceId", required = false) UUID datasourceId,
            @AuthenticationPrincipal DavsUser actor
    );


    @PostMapping("/policies")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<RowFilterDto> saveRowFilter(
            @Valid @RequestBody SaveRowFilterRequest request,
            @AuthenticationPrincipal DavsUser actor
    );


    @DeleteMapping("/policies/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<Void> deleteRowFilter(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    );
}
