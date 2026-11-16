package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/collections/{id}/items} (UC06 [6d]).
 */
public record AddCollectionItemRequest(
        @NotBlank(message = "resourceType là bắt buộc")
        String resourceType,
        @NotNull(message = "resourceId là bắt buộc")
        UUID resourceId
) {
}
