package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.SiteRole;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code PATCH /api/v1/users/{id}/role} (UC03).
 */
public record UpdateUserRoleRequest(
        @NotNull(message = "Vai trò không được để trống")
        SiteRole role
) {
}
