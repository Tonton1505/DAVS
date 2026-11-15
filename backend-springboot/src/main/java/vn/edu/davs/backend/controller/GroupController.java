package vn.edu.davs.backend.controller;

import vn.edu.davs.backend.dto.AddGroupMembersRequest;
import vn.edu.davs.backend.dto.CreateGroupRequest;
import vn.edu.davs.backend.dto.GroupDetailDto;
import vn.edu.davs.backend.dto.GroupDto;
import vn.edu.davs.backend.dto.UpdateGroupRequest;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.GroupService;
import vn.edu.davs.backend.service.PermissionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only REST endpoints for UC04 Workgroup Management ({@code /api/v1/groups}).
 */
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<List<GroupDto>> listGroups(
            @AuthenticationPrincipal DavsUser actor,
            @RequestParam(value = "q", required = false) String q
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(groupService.listGroups(q));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<GroupDetailDto> getGroup(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(groupService.getGroupDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<GroupDetailDto> createGroup(
            @AuthenticationPrincipal DavsUser actor,
            @RequestBody CreateGroupRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        GroupDetailDto created = groupService.createGroup(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<GroupDetailDto> updateGroup(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestBody UpdateGroupRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(groupService.updateGroup(id, request, actor));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<Void> deleteGroup(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        groupService.deleteGroup(id, actor);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<GroupDetailDto> addMembers(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @Valid @RequestBody AddGroupMembersRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(groupService.addMembers(id, request.userIds(), actor));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<GroupDetailDto> removeMember(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @PathVariable UUID userId
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(groupService.removeMember(id, userId, actor));
    }
}
