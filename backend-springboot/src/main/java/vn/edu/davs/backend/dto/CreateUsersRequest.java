package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.AuthMethod;

import vn.edu.davs.backend.enu.SiteRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request body for {@code POST /api/v1/users} (UC02 Add Users by Username).
 */
public record CreateUsersRequest(
        @NotNull(message = "Phương thức xác thực là bắt buộc")
        AuthMethod authMethod,
        @NotEmpty(message = "Cần ít nhất một tên tài khoản")
        @Size(max = 50, message = "Tối đa 50 tên tài khoản mỗi lần")
        List<String> usernames,
        @NotNull(message = "Vai trò trên site là bắt buộc")
        SiteRole role
) {
}
