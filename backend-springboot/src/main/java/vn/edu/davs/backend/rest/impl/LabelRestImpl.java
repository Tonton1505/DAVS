package vn.edu.davs.backend.rest.impl;

import vn.edu.davs.backend.dto.CreateLabelCategoryRequest;
import vn.edu.davs.backend.dto.CreateLabelRequest;
import vn.edu.davs.backend.dto.LabelCategoryDto;
import vn.edu.davs.backend.dto.LabelDto;
import vn.edu.davs.backend.dto.LabelTargetRequest;
import vn.edu.davs.backend.dto.UpdateLabelRequest;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.LabelService;
import jakarta.validation.Valid;
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
import vn.edu.davs.backend.rest.LabelRest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class LabelRestImpl implements LabelRest {


    private final LabelService labelService;

    @Override
    public ResponseEntity<List<LabelCategoryDto>> listCategories() {
        return ResponseEntity.ok(labelService.listCategories());
    }

    @Override
    public ResponseEntity<LabelCategoryDto> createCategory(
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody CreateLabelCategoryRequest request
    ) {
        LabelCategoryDto created = labelService.createCategory(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<List<LabelDto>> listLabels(
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @RequestParam(value = "q", required = false) String query
    ) {
        return ResponseEntity.ok(labelService.listLabels(categoryId, query));
    }

    @Override
    public ResponseEntity<LabelDto> createLabel(
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody CreateLabelRequest request
    ) {
        LabelDto created = labelService.createLabel(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<LabelDto> updateLabel(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody UpdateLabelRequest request
    ) {
        return ResponseEntity.ok(labelService.updateLabel(id, request, actor));
    }

    @Override
    public ResponseEntity<Map<String, Object>> deleteLabel(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(labelService.deleteLabel(id, actor));
    }

    @Override
    public ResponseEntity<LabelDto> assignLabel(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody LabelTargetRequest request
    ) {
        return ResponseEntity.ok(labelService.assignLabel(id, request, actor));
    }

    @Override
    public ResponseEntity<LabelDto> unassignLabel(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody LabelTargetRequest request
    ) {
        return ResponseEntity.ok(labelService.unassignLabel(id, request, actor));
    }

    @Override
    public ResponseEntity<List<LabelDto>> listLabelsForTarget(
            @RequestParam("type") String type,
            @RequestParam("id") UUID id
    ) {
        return ResponseEntity.ok(labelService.listLabelsForTarget(type, id));
    }
}
