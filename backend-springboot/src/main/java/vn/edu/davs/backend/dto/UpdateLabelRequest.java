package vn.edu.davs.backend.dto;

/**
 * Request body for {@code PATCH /api/v1/labels/{id}} (UC09).
 */
public record UpdateLabelRequest(
        String value,
        String description,
        Boolean isWarning
) {
}
