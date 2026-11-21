package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.PageResponse;
import vn.edu.davs.backend.dto.UserManagementDto;
import vn.edu.davs.backend.enu.SiteRole;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC repository for UC03 user listing, role updates, activation/deactivation,
 * invitation resend, and content ownership transfer on deletion.
 */
public interface IUserManagementDao {


    PageResponse<UserManagementDto> searchUsers(
            String q,
            String role,
            String group,
            String status,
            int page,
            int size
    );


    Optional<UserManagementDto> findUserById(UUID userId);


    int countActiveSiteAdmins();


    int countOwnedResources(UUID userId);


    boolean reserveLicense(SiteRole role, int count);


    void releaseLicense(SiteRole role, int count);


    void updateUserRole(UUID userId, SiteRole newRole);


    void updateUserStatus(UUID userId, String status);


    void revokeAllRefreshTokens(UUID userId);


    void transferOwnership(UUID fromUserId, UUID toUserId);


    void softDeleteUser(UUID userId);
}
