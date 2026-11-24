package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.CreateLabelCategoryRequest;
import vn.edu.davs.backend.dto.CreateLabelRequest;
import vn.edu.davs.backend.dto.LabelCategoryDto;
import vn.edu.davs.backend.dto.LabelDto;
import vn.edu.davs.backend.dto.LabelTargetRequest;
import vn.edu.davs.backend.dto.UpdateLabelRequest;
import vn.edu.davs.backend.model.DavsUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST endpoints for UC09 Data Labels &amp; Categories ({@code /api/v1/label-categories}, {@code /api/v1/labels}).
 */
public interface LabelRest {


    @GetMapping("/label-categories")
    ResponseEntity<List<LabelCategoryDto>> listCategories();


    @PostMapping("/label-categories")
    ResponseEntity<LabelCategoryDto> createCategory(
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody CreateLabelCategoryRequest request
    );


    @GetMapping("/labels")
    ResponseEntity<List<LabelDto>> listLabels(
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @RequestParam(value = "q", required = false) String query
    );


    @PostMapping("/labels")
    ResponseEntity<LabelDto> createLabel(
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody CreateLabelRequest request
    );


    @PatchMapping("/labels/{id}")
    ResponseEntity<LabelDto> updateLabel(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody UpdateLabelRequest request
    );


    @DeleteMapping("/labels/{id}")
    ResponseEntity<Map<String, Object>> deleteLabel(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    );


    @PostMapping("/labels/{id}/assign")
    ResponseEntity<LabelDto> assignLabel(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody LabelTargetRequest request
    );


    @PostMapping("/labels/{id}/unassign")
    ResponseEntity<LabelDto> unassignLabel(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody LabelTargetRequest request
    );


    @GetMapping("/labels/targets")
    ResponseEntity<List<LabelDto>> listLabelsForTarget(
            @RequestParam("type") String type,
            @RequestParam("id") UUID id
    );
}
