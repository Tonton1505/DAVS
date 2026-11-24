package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.PublishWorkbookRequest;
import vn.edu.davs.backend.dto.PublishWorkbookResponse;
import vn.edu.davs.backend.model.DavsUser;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * REST controller for UC21: Publish Workbook (FR-SHR-01, URD-14, SCR-21).
 * Supports Internal Server, Cloud, and Public destinations with permission and security validation.
 */
public interface PublishRest {


    @PostMapping("/{id}/publish")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<PublishWorkbookResponse> publishWorkbook(
            @PathVariable("id") UUID workbookId,
            @RequestBody PublishWorkbookRequest request,
            @AuthenticationPrincipal DavsUser actor
    );


    @GetMapping("/{id}/publish/targets")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<Map<String, Object>> getPublishTargets(
            @PathVariable("id") UUID workbookId,
            @AuthenticationPrincipal DavsUser actor
    );
}
