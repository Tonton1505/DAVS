package vn.edu.davs.backend.rest.impl;

import vn.edu.davs.backend.dto.BatchUpdateUserRoleRequest;
import vn.edu.davs.backend.dto.PageResponse;
import vn.edu.davs.backend.dto.UpdateUserRoleRequest;
import vn.edu.davs.backend.dto.UserManagementDto;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.PermissionService;
import vn.edu.davs.backend.service.UserManagementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.davs.backend.rest.UserManagementRest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserManagementRestImpl implements UserManagementRest {


    private final UserManagementService userManagementService;

    private final PermissionService permissionService;

    @Override
    public ResponseEntity<PageResponse<UserManagementDto>> listUsers(
            @AuthenticationPrincipal DavsUser actor,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(userManagementService.searchUsers(q, role, group, status, page, size));
    }

    @Override
    public ResponseEntity<UserManagementDto> getUser(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(userManagementService.getUser(id));
    }

    @Override
    public ResponseEntity<UserManagementDto> updateRole(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(userManagementService.changeRole(id, request.role(), actor));
    }

    @Override
    public ResponseEntity<List<UserManagementDto>> batchUpdateRole(
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody BatchUpdateUserRoleRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(userManagementService.batchChangeRole(request.userIds(), request.role(), actor));
    }

    @Override
    public ResponseEntity<UserManagementDto> deactivateUser(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(userManagementService.deactivateUser(id, actor));
    }

    @Override
    public ResponseEntity<UserManagementDto> activateUser(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(userManagementService.activateUser(id, actor));
    }

    @Override
    public ResponseEntity<UserManagementDto> resendInvite(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(userManagementService.resendInvite(id, actor));
    }

    @Override
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestParam(value = "newOwnerId", required = false) UUID newOwnerId
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        userManagementService.deleteUser(id, newOwnerId, actor);
        return ResponseEntity.noContent().build();
    }
}
