package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Representation of a calculated field in a data source (UC16).
 */
public record CalculatedFieldDto(
        UUID id,
        UUID datasourceId,
        String name,
        String formula,
        String returnType,
        String role,
        Instant createdAt,
        Instant updatedAt
) {
}
