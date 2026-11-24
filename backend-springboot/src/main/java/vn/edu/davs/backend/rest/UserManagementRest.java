package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.BatchUpdateUserRoleRequest;
import vn.edu.davs.backend.dto.PageResponse;
import vn.edu.davs.backend.dto.UpdateUserRoleRequest;
import vn.edu.davs.backend.dto.UserManagementDto;
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
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin-only REST endpoints for UC03 User &amp; Role Management ({@code /api/v1/users}).
 */
public interface UserManagementRest {


    @GetMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<PageResponse<UserManagementDto>> listUsers(
            @AuthenticationPrincipal DavsUser actor,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size
    );


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<UserManagementDto> getUser(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    );


    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<UserManagementDto> updateRole(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request
    );


    @PatchMapping("/batch-role")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<List<UserManagementDto>> batchUpdateRole(
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody BatchUpdateUserRoleRequest request
    );


    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<UserManagementDto> deactivateUser(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    );


    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<UserManagementDto> activateUser(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    );


    @PostMapping("/{id}/resend-invite")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<UserManagementDto> resendInvite(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id
    );


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID id,
            @RequestParam(value = "newOwnerId", required = false) UUID newOwnerId
    );
}
