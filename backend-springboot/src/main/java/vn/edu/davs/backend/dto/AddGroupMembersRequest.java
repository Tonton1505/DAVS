package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/groups/{id}/members} (UC04).
 */
public record AddGroupMembersRequest(
        @NotEmpty(message = "Danh sách thành viên không được để trống")
        List<UUID> userIds
) {
}
