package vn.edu.davs.backend.dto;

import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/projects/{id}/move} (UC06 [6a]).
 */
public record MoveProjectRequest(
        UUID targetId
) {
}
