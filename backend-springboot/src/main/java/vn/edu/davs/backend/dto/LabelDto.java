package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * API representation of a data label (UC09).
 */
public record LabelDto(
        UUID id,
        UUID categoryId,
        String categoryName,
        String value,
        String description,
        boolean isWarning,
        int assignedCount,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
