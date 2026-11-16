package vn.edu.davs.backend.dto;

import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/labels} (UC09).
 */
public record CreateLabelRequest(
        UUID categoryId,
        String value,
        String description,
        Boolean isWarning
) {
}
