package vn.edu.davs.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Representation of a workbook parameter (UC16).
 */
public record ParameterDto(
        UUID id,
        UUID workbookId,
        String name,
        String dataType,
        String allowableValuesType,
        List<String> listValues,
        BigDecimal rangeMin,
        BigDecimal rangeMax,
        BigDecimal rangeStep,
        String currentValue,
        Instant createdAt,
        Instant updatedAt
) {
    public ParameterDto {
        listValues = listValues == null ? List.of() : List.copyOf(listValues);
    }
}
