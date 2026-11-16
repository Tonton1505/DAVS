package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.SiteRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Request body for batch role change {@code PATCH /api/v1/users/batch-role} (UC03).
 */
public record BatchUpdateUserRoleRequest(
        @NotEmpty(message = "Danh sách người dùng không được để trống")
        List<UUID> userIds,
        @NotNull(message = "Vai trò không được để trống")
        SiteRole role
) {
}
