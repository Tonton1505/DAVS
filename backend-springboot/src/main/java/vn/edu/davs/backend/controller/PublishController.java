package vn.edu.davs.backend.controller;

import vn.edu.davs.backend.dto.PublishWorkbookRequest;
import vn.edu.davs.backend.dto.PublishWorkbookResponse;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.PublishService;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for UC21: Publish Workbook (FR-SHR-01, SCR-21).
 */
@RestController
@RequestMapping("/api/v1/workbooks")
@RequiredArgsConstructor
public class PublishController {

    private final PublishService publishService;

    @PostMapping("/{id}/publish")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PublishWorkbookResponse> publishWorkbook(
            @PathVariable("id") UUID workbookId,
            @RequestBody PublishWorkbookRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        PublishWorkbookResponse response = publishService.publishWorkbook(workbookId, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/publish/targets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getPublishTargets(
            @PathVariable("id") UUID workbookId,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(publishService.getPublishTargets(workbookId, actor));
    }
}
