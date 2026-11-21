package vn.edu.davs.backend.rest.impl;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.rest.ContentRest;

@RequiredArgsConstructor
@RestController
public class ContentRestImpl implements ContentRest {


    private final ProjectService projectService;

    @Override
    public ResponseEntity<ContentListResponse> getContent(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "sort", required = false, defaultValue = "updatedAt") String sort,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(projectService.getContent(projectId, query, sort, actor));
    }

    @Override
    public ResponseEntity<List<CollectionDto>> listCollections(
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(projectService.listCollections(actor));
    }

    @Override
    public ResponseEntity<CollectionDto> createCollection(
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody CreateCollectionRequest request
    ) {
        CollectionDto created = projectService.createCollection(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<CollectionDto> addCollectionItem(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody AddCollectionItemRequest request
    ) {
        return ResponseEntity.ok(projectService.addCollectionItem(id, request, actor));
    }
}
