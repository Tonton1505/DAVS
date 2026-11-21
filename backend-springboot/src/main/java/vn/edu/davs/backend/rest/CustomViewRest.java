package vn.edu.davs.backend.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import vn.edu.davs.backend.dto.CreateCustomViewRequest;
import vn.edu.davs.backend.dto.CustomViewDto;
import vn.edu.davs.backend.dto.SaveAsWorkbookRequest;
import vn.edu.davs.backend.dto.WorkbookDto;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "UC20 - Ad-hoc Exploration & Custom Views", description = "Custom views, default views, sharing and save-as workbook")
@RequestMapping("/api/v1")
public interface CustomViewRest {


    @Operation(summary = "List custom views visible to user on dashboard")
    @GetMapping("/dashboards/{dashboardId}/custom-views")
    ResponseEntity<List<CustomViewDto>> listCustomViews(
            @PathVariable UUID dashboardId,
            @AuthenticationPrincipal DavsUser actor
    );


    @Operation(summary = "Create custom view")
    @PostMapping("/custom-views")
    ResponseEntity<CustomViewDto> createCustomView(
            @Valid @RequestBody CreateCustomViewRequest request,
            @AuthenticationPrincipal DavsUser actor
    );


    @Operation(summary = "Set custom view as default for author")
    @PutMapping("/custom-views/{id}/default")
    ResponseEntity<Map<String, String>> setDefault(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    );


    @Operation(summary = "Update sharing status of custom view")
    @PostMapping("/custom-views/{id}/share")
    ResponseEntity<Map<String, String>> setShared(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean shared,
            @AuthenticationPrincipal DavsUser actor
    );


    @Operation(summary = "Delete custom view")
    @DeleteMapping("/custom-views/{id}")
    ResponseEntity<Void> deleteCustomView(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    );


    @Operation(summary = "Save copy of workbook into target project (ad-hoc save-as)")
    @PostMapping("/workbooks/save-as")
    ResponseEntity<WorkbookDto> saveAsWorkbook(
            @Valid @RequestBody SaveAsWorkbookRequest request,
            @AuthenticationPrincipal DavsUser actor
    );
}
