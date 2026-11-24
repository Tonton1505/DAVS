package vn.edu.davs.backend.rest.impl;

import vn.edu.davs.backend.dto.PublishWorkbookRequest;
import vn.edu.davs.backend.dto.PublishWorkbookResponse;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.PublishService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.davs.backend.rest.PublishRest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workbooks")
public class PublishRestImpl implements PublishRest {


    private final PublishService publishService;

    @Override
    public ResponseEntity<PublishWorkbookResponse> publishWorkbook(
            @PathVariable("id") UUID workbookId,
            @RequestBody PublishWorkbookRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        PublishWorkbookResponse response = publishService.publishWorkbook(workbookId, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<Map<String, Object>> getPublishTargets(
            @PathVariable("id") UUID workbookId,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(publishService.getPublishTargets(workbookId, actor));
    }
}
