package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Row-Level Security (RLS) policy mapping DTO (T-101).
 */
public record RowFilterDto(
        UUID id,
        UUID datasourceId,
        String datasourceName,
        String subjectType,
        UUID subjectId,
        String subjectName,
        String fieldName,
        List<String> allowedValues,
        Instant createdAt,
        Instant updatedAt
) {
    public RowFilterDto {
        allowedValues = allowedValues == null ? List.of() : List.copyOf(allowedValues);
    }
}
