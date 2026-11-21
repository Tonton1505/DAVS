package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.AddGroupMembersRequest;
import vn.edu.davs.backend.dto.CreateGroupRequest;
import vn.edu.davs.backend.dto.GroupDetailDto;
import vn.edu.davs.backend.dto.GroupDto;
import vn.edu.davs.backend.dto.UpdateGroupRequest;
import vn.edu.davs.backend.model.DavsUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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

/**
 * Admin-only REST endpoints for UC04 Workgroup Management ({@code /api/v1/groups}).
 */
@RequestMapping("/api/v1/groups")
public interface GroupRest {


    @GetMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<List<GroupDto>> listGroups(
            @AuthenticationPrincipal DavsUser actor,
            @RequestParam(value = "q", required = false) String q
    );


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<GroupDetailDto> getGroup(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    );


    @PostMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<GroupDetailDto> createGroup(
            @AuthenticationPrincipal DavsUser actor,
            @RequestBody CreateGroupRequest request
    );


    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<GroupDetailDto> updateGroup(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestBody UpdateGroupRequest request
    );


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<Void> deleteGroup(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    );


    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<GroupDetailDto> addMembers(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @Valid @RequestBody AddGroupMembersRequest request
    );


    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<GroupDetailDto> removeMember(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @PathVariable UUID userId
    );
}
