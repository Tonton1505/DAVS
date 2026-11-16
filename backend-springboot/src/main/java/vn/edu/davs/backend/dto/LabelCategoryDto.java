package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * API representation of a data label category (UC09).
 */
public record LabelCategoryDto(
        UUID id,
        String name,
        String description,
        boolean isWarning,
        UUID createdBy,
        Instant createdAt
) {
}
