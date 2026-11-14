package com.davs.project;

import com.davs.common.security.DavsUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for UC07 resource permission rules, templates, and effective-permission checks.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ResourcePermissionsController {

    private final ResourcePermissionService resourcePermissionService;

    @GetMapping("/permission-templates")
    public List<PermissionTemplateDto> listTemplates() {
        return resourcePermissionService.listTemplates();
    }

    @GetMapping("/resources/{type}/{id}/permissions")
    public ResourcePermissionsResponse getPermissions(
            @PathVariable String type,
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return resourcePermissionService.getPermissions(type, id, actor);
    }

    @PutMapping("/resources/{type}/{id}/permissions")
    public UpdateResourcePermissionsResponse updatePermissions(
            @PathVariable String type,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateResourcePermissionsRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return resourcePermissionService.updatePermissions(type, id, request, actor);
    }

    @GetMapping("/resources/{type}/{id}/permissions/effective")
    public EffectivePermissionsResponse getEffectivePermissions(
            @PathVariable String type,
            @PathVariable UUID id,
            @RequestParam UUID userId,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return resourcePermissionService.getEffectivePermissions(type, id, userId, actor);
    }

    @GetMapping("/resources/{type}/{id}/permissions/subjects")
    public List<PermissionSubjectDto> listSubjects(
            @PathVariable String type,
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return resourcePermissionService.listSubjects(type, id, actor);
    }
}
