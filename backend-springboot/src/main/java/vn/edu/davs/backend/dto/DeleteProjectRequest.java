package vn.edu.davs.backend.dto;

/**
 * Request body for {@code DELETE /api/v1/projects/{id}} (UC06 [6c]).
 */
public record DeleteProjectRequest(
        String confirmName
) {
}
