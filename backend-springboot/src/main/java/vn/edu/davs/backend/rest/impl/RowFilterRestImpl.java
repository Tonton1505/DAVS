package vn.edu.davs.backend.rest.impl;

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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.davs.backend.rest.RowFilterRest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/rls")
public class RowFilterRestImpl implements RowFilterRest {


    private final RowFilterService rowFilterService;

    @Override
    public ResponseEntity<List<DatasourceRlsStatusDto>> listDatasourceRlsStatuses(
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(rowFilterService.listDatasourceRlsStatuses(actor));
    }

    @Override
    public ResponseEntity<DatasourceRlsStatusDto> toggleDatasourceRls(
            @PathVariable UUID datasourceId,
            @RequestBody ToggleDatasourceRlsRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(rowFilterService.toggleDatasourceRls(datasourceId, request.enabled(), actor));
    }

    @Override
    public ResponseEntity<List<RowFilterDto>> listRowFilters(
            @RequestParam(value = "datasourceId", required = false) UUID datasourceId,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(rowFilterService.listRowFilters(datasourceId, actor));
    }

    @Override
    public ResponseEntity<RowFilterDto> saveRowFilter(
            @Valid @RequestBody SaveRowFilterRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rowFilterService.saveRowFilter(request, actor));
    }

    @Override
    public ResponseEntity<Void> deleteRowFilter(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        rowFilterService.deleteRowFilter(id, actor);
        return ResponseEntity.noContent().build();
    }
}
