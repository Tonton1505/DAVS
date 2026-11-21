package vn.edu.davs.backend.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "UC20 - Ad-hoc Exploration & Custom Views", description = "Custom views, default views, sharing and save-as workbook")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CustomViewController {

    private final CustomViewService customViewService;

    @Operation(summary = "List custom views visible to user on dashboard")
    @GetMapping("/dashboards/{dashboardId}/custom-views")
    public ResponseEntity<List<CustomViewDto>> listCustomViews(
            @PathVariable UUID dashboardId,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(customViewService.listVisible(dashboardId, actor));
    }

    @Operation(summary = "Create custom view")
    @PostMapping("/custom-views")
    public ResponseEntity<CustomViewDto> createCustomView(
            @Valid @RequestBody CreateCustomViewRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customViewService.create(request, actor));
    }

    @Operation(summary = "Set custom view as default for author")
    @PutMapping("/custom-views/{id}/default")
    public ResponseEntity<Map<String, String>> setDefault(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        customViewService.setDefault(id, actor);
        return ResponseEntity.ok(Map.of("message", "Đã đặt làm view mặc định"));
    }

    @Operation(summary = "Update sharing status of custom view")
    @RequestMapping(value = "/custom-views/{id}/share", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Map<String, String>> setShared(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean shared,
            @AuthenticationPrincipal DavsUser actor
    ) {
        customViewService.setShared(id, shared, actor);
        return ResponseEntity.ok(Map.of("message", shared ? "Đã chia sẻ view" : "Đã chuyển về riêng tư"));
    }

    @Operation(summary = "Delete custom view")
    @DeleteMapping("/custom-views/{id}")
    public ResponseEntity<Void> deleteCustomView(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        customViewService.delete(id, actor);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Save copy of workbook into target project (ad-hoc save-as)")
    @PostMapping({"/workbooks/save-as", "/dashboards/{id}/save-as", "/dashboards/save-as"})
    public ResponseEntity<WorkbookDto> saveAsWorkbook(
            @Valid @RequestBody SaveAsWorkbookRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customViewService.saveAsWorkbook(request, actor));
    }
}
