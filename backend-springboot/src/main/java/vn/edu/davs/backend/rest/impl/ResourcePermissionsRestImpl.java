package vn.edu.davs.backend.rest.impl;

import vn.edu.davs.backend.dto.EffectivePermissionsResponse;
import vn.edu.davs.backend.dto.PermissionSubjectDto;
import vn.edu.davs.backend.dto.PermissionTemplateDto;
import vn.edu.davs.backend.dto.ResourcePermissionsResponse;
import vn.edu.davs.backend.dto.UpdateResourcePermissionsRequest;
import vn.edu.davs.backend.dto.UpdateResourcePermissionsResponse;
import vn.edu.davs.backend.service.ResourcePermissionService;
import vn.edu.davs.backend.model.DavsUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.davs.backend.rest.ResourcePermissionsRest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class ResourcePermissionsRestImpl implements ResourcePermissionsRest {


    private final ResourcePermissionService resourcePermissionService;

    @Override
    public List<PermissionTemplateDto> listTemplates() {
        return resourcePermissionService.listTemplates();
    }

    @Override
    public ResourcePermissionsResponse getPermissions(
            @PathVariable String type,
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return resourcePermissionService.getPermissions(type, id, actor);
    }

    @Override
    public UpdateResourcePermissionsResponse updatePermissions(
            @PathVariable String type,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateResourcePermissionsRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return resourcePermissionService.updatePermissions(type, id, request, actor);
    }

    @Override
    public EffectivePermissionsResponse getEffectivePermissions(
            @PathVariable String type,
            @PathVariable UUID id,
            @RequestParam UUID userId,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return resourcePermissionService.getEffectivePermissions(type, id, userId, actor);
    }

    @Override
    public List<PermissionSubjectDto> listSubjects(
            @PathVariable String type,
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return resourcePermissionService.listSubjects(type, id, actor);
    }
}
