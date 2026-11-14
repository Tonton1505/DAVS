package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.Size;

/**
 * Request body for {@code PATCH /api/v1/projects/{id}} (UC06).
 */
public record UpdateProjectRequest(
        String name,
        @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
        String description
) {
}
