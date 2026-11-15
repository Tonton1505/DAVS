package vn.edu.davs.backend.dto;

import java.util.List;
import java.util.Map;

/**
 * Response body for {@code POST /api/v1/prep-flows/{id}/preview} (UC12, BUS-DAT-05, BUS-DAT-06).
 */
public record PrepPreviewResponse(
        int stepIndex,
        long inputRowCount,
        long rowCount,
        long duplicateRowCount,
        long replacedNullsCount,
        List<ColumnProfileDto> profile,
        List<PrepFlowDto.LineageStepDto> lineage,
        List<Map<String, Object>> rows
) {
    public PrepPreviewResponse {
        profile = profile == null ? List.of() : List.copyOf(profile);
        lineage = lineage == null ? List.of() : List.copyOf(lineage);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    public record ColumnProfileDto(
            String fieldName,
            String dataType,
            String role,
            long rowCount,
            long nullCount,
            double nullPercent,
            long distinctCount,
            long duplicateCount,
            String minValue,
            String maxValue
    ) {
    }
}
