package vn.edu.davs.backend.controller;

import vn.edu.davs.backend.dto.AddCollectionItemRequest;
import vn.edu.davs.backend.dto.CollectionDto;
import vn.edu.davs.backend.dto.ContentListResponse;
import vn.edu.davs.backend.dto.CreateCollectionRequest;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for UC06 Content Browsing ({@code /api/v1/content})
 * and Personal Collections ({@code /api/v1/collections}).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ContentController {

    private final ProjectService projectService;

    @GetMapping("/content")
    public ResponseEntity<ContentListResponse> getContent(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "sort", required = false, defaultValue = "updatedAt") String sort,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(projectService.getContent(projectId, query, sort, actor));
    }

    @GetMapping("/collections")
    public ResponseEntity<List<CollectionDto>> listCollections(
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(projectService.listCollections(actor));
    }

    @PostMapping("/collections")
    public ResponseEntity<CollectionDto> createCollection(
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody CreateCollectionRequest request
    ) {
        CollectionDto created = projectService.createCollection(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/collections/{id}/items")
    public ResponseEntity<CollectionDto> addCollectionItem(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody AddCollectionItemRequest request
    ) {
        return ResponseEntity.ok(projectService.addCollectionItem(id, request, actor));
    }
}
