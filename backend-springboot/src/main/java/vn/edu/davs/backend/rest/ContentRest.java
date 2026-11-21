package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.AddCollectionItemRequest;
import vn.edu.davs.backend.dto.CollectionDto;
import vn.edu.davs.backend.dto.ContentListResponse;
import vn.edu.davs.backend.dto.CreateCollectionRequest;
import vn.edu.davs.backend.model.DavsUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST endpoints for UC06 Content Browsing ({@code /api/v1/content})
 * and Personal Collections ({@code /api/v1/collections}).
 */
@RequestMapping("/api/v1")
public interface ContentRest {


    @GetMapping("/content")
    ResponseEntity<ContentListResponse> getContent(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "sort", required = false, defaultValue = "updatedAt") String sort,
            @AuthenticationPrincipal DavsUser actor
    );


    @GetMapping("/collections")
    ResponseEntity<List<CollectionDto>> listCollections(
            @AuthenticationPrincipal DavsUser actor
    );


    @PostMapping("/collections")
    ResponseEntity<CollectionDto> createCollection(
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody CreateCollectionRequest request
    );


    @PostMapping("/collections/{id}/items")
    ResponseEntity<CollectionDto> addCollectionItem(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody AddCollectionItemRequest request
    );
}
