package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.PageResponse;
import vn.edu.davs.backend.dto.UserManagementDto;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core UC03 business logic: user search/filtering, role upgrades/downgrades,
 * activation/deactivation, invitation resend, and content ownership transfer upon deletion.
 */
public interface UserManagementService {


    @Transactional(readOnly = true)
    PageResponse<UserManagementDto> searchUsers(
            String q,
            String role,
            String group,
            String status,
            int page,
            int size
    );


    @Transactional(readOnly = true)
    UserManagementDto getUser(UUID userId);


    @Transactional
    UserManagementDto changeRole(UUID userId, SiteRole newRole, DavsUser actor);


    @Transactional
    List<UserManagementDto> batchChangeRole(List<UUID> userIds, SiteRole newRole, DavsUser actor);


    @Transactional
    UserManagementDto deactivateUser(UUID userId, DavsUser actor);


    @Transactional
    UserManagementDto activateUser(UUID userId, DavsUser actor);


    @Transactional
    UserManagementDto resendInvite(UUID userId, DavsUser actor);


    @Transactional
    void deleteUser(UUID userId, UUID newOwnerId, DavsUser actor);
}
