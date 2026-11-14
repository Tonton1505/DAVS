package com.davs.project;

import com.davs.permission.PermissionMode;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/projects} (UC05).
 */
public record CreateProjectRequest(
        String name,
        @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
        String description,
        UUID parentId,
        PermissionMode permissionMode
) {
}
