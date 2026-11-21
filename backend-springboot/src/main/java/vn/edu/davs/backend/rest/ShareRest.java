package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.DirectoryItemDto;
import vn.edu.davs.backend.dto.ShareRequest;
import vn.edu.davs.backend.dto.ShareResponse;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST controller for UC22: Content Sharing (FR-SHR-02, URD-15, SCR-22).
 * Supports recipient sharing, permission level ceilings, and embed link generation.
 */
@RequestMapping("/api/v1")
public interface ShareRest {


    @PostMapping("/shares")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<ShareResponse> shareContent(
            @RequestBody ShareRequest request,
            @AuthenticationPrincipal DavsUser actor
    );


    @GetMapping("/shares/link")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<Map<String, Object>> getShareLink(
            @RequestParam(value = "resourceType", defaultValue = "WORKBOOK") String resourceType,
            @RequestParam("resourceId") UUID resourceId,
            @AuthenticationPrincipal DavsUser actor
    );


    @GetMapping("/directory")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<List<DirectoryItemDto>> searchDirectory(
            @RequestParam(value = "q", required = false) String query,
            @AuthenticationPrincipal DavsUser actor
    );
}
