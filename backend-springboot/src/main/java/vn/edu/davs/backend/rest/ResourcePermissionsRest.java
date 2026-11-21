package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.EffectivePermissionsResponse;
import vn.edu.davs.backend.dto.PermissionSubjectDto;
import vn.edu.davs.backend.dto.PermissionTemplateDto;
import vn.edu.davs.backend.dto.ResourcePermissionsResponse;
import vn.edu.davs.backend.dto.UpdateResourcePermissionsRequest;
import vn.edu.davs.backend.dto.UpdateResourcePermissionsResponse;
import vn.edu.davs.backend.model.DavsUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST endpoints for UC07 resource permission rules, templates, and effective-permission checks.
 */
@RequestMapping("/api/v1")
public interface ResourcePermissionsRest {


    @GetMapping("/permission-templates")
    List<PermissionTemplateDto> listTemplates();


    @GetMapping("/resources/{type}/{id}/permissions")
    ResourcePermissionsResponse getPermissions(
            @PathVariable String type,
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    );


    @PutMapping("/resources/{type}/{id}/permissions")
    UpdateResourcePermissionsResponse updatePermissions(
            @PathVariable String type,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateResourcePermissionsRequest request,
            @AuthenticationPrincipal DavsUser actor
    );


    @GetMapping("/resources/{type}/{id}/permissions/effective")
    EffectivePermissionsResponse getEffectivePermissions(
            @PathVariable String type,
            @PathVariable UUID id,
            @RequestParam UUID userId,
            @AuthenticationPrincipal DavsUser actor
    );


    @GetMapping("/resources/{type}/{id}/permissions/subjects")
    List<PermissionSubjectDto> listSubjects(
            @PathVariable String type,
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    );
}
