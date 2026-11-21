package vn.edu.davs.backend.rest.impl;

import vn.edu.davs.backend.dto.DirectoryItemDto;
import vn.edu.davs.backend.dto.ShareRequest;
import vn.edu.davs.backend.dto.ShareResponse;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.ShareService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.rest.ShareRest;

@RequiredArgsConstructor
@RestController
public class ShareRestImpl implements ShareRest {


    private final ShareService shareService;

    @Override
    public ResponseEntity<ShareResponse> shareContent(
            @RequestBody ShareRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        ShareResponse response = shareService.shareContent(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<Map<String, Object>> getShareLink(
            @RequestParam(value = "resourceType", defaultValue = "WORKBOOK") String resourceType,
            @RequestParam("resourceId") UUID resourceId,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(shareService.getShareLink(resourceType, resourceId, actor));
    }

    @Override
    public ResponseEntity<List<DirectoryItemDto>> searchDirectory(
            @RequestParam(value = "q", required = false) String query,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(shareService.searchDirectory(query, actor));
    }
}
