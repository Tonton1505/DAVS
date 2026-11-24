package vn.edu.davs.backend.rest.impl;

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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.davs.backend.rest.GroupRest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/groups")
public class GroupRestImpl implements GroupRest {


    private final GroupService groupService;

    private final PermissionService permissionService;

    @Override
    public ResponseEntity<List<GroupDto>> listGroups(
            @AuthenticationPrincipal DavsUser actor,
            @RequestParam(value = "q", required = false) String q
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(groupService.listGroups(q));
    }

    @Override
    public ResponseEntity<GroupDetailDto> getGroup(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(groupService.getGroupDetail(id));
    }

    @Override
    public ResponseEntity<GroupDetailDto> createGroup(
            @AuthenticationPrincipal DavsUser actor,
            @RequestBody CreateGroupRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        GroupDetailDto created = groupService.createGroup(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<GroupDetailDto> updateGroup(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestBody UpdateGroupRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(groupService.updateGroup(id, request, actor));
    }

    @Override
    public ResponseEntity<Void> deleteGroup(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        groupService.deleteGroup(id, actor);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<GroupDetailDto> addMembers(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @Valid @RequestBody AddGroupMembersRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(groupService.addMembers(id, request.userIds(), actor));
    }

    @Override
    public ResponseEntity<GroupDetailDto> removeMember(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @PathVariable UUID userId
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(groupService.removeMember(id, userId, actor));
    }
}
