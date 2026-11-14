package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/projects/{id}/owner} (UC06 [6b]).
 */
public record ChangeProjectOwnerRequest(
        @NotNull(message = "userId là bắt buộc")
        UUID userId
) {
}
