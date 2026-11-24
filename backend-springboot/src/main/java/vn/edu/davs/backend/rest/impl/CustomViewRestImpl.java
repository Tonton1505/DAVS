package vn.edu.davs.backend.rest.impl;

import jakarta.validation.Valid;
import vn.edu.davs.backend.dto.CreateCustomViewRequest;
import vn.edu.davs.backend.dto.CustomViewDto;
import vn.edu.davs.backend.dto.SaveAsWorkbookRequest;
import vn.edu.davs.backend.dto.WorkbookDto;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.CustomViewService;
import java.util.List;
import java.util.Map;
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
import vn.edu.davs.backend.rest.CustomViewRest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class CustomViewRestImpl implements CustomViewRest {


    private final CustomViewService customViewService;

    @Override
    public ResponseEntity<List<CustomViewDto>> listCustomViews(
            @PathVariable UUID dashboardId,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(customViewService.listVisible(dashboardId, actor));
    }

    @Override
    public ResponseEntity<CustomViewDto> createCustomView(
            @Valid @RequestBody CreateCustomViewRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customViewService.create(request, actor));
    }

    @Override
    public ResponseEntity<Map<String, String>> setDefault(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        customViewService.setDefault(id, actor);
        return ResponseEntity.ok(Map.of("message", "Đã đặt làm view mặc định"));
    }

    @Override
    public ResponseEntity<Map<String, String>> setShared(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean shared,
            @AuthenticationPrincipal DavsUser actor
    ) {
        customViewService.setShared(id, shared, actor);
        return ResponseEntity.ok(Map.of("message", shared ? "Đã chia sẻ view" : "Đã chuyển về riêng tư"));
    }

    @Override
    public ResponseEntity<Void> deleteCustomView(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        customViewService.delete(id, actor);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<WorkbookDto> saveAsWorkbook(
            @Valid @RequestBody SaveAsWorkbookRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customViewService.saveAsWorkbook(request, actor));
    }
}
