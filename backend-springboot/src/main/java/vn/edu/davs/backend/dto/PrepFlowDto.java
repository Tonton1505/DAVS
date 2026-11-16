package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data preparation flow DTO returned by {@code /api/v1/prep-flows} (UC12).
 */
public record PrepFlowDto(
        UUID id,
        UUID datasourceId,
        UUID outputDatasourceId,
        String name,
        String description,
        List<PrepStepDto> steps,
        List<LineageStepDto> lineage,
        UUID ownerId,
        boolean published,
        Instant createdAt,
        Instant updatedAt
) {
    public PrepFlowDto {
        steps = steps == null ? List.of() : List.copyOf(steps);
        lineage = lineage == null ? List.of() : List.copyOf(lineage);
    }

    public record PrepStepDto(
            String id,
            String type,
            String name,
            Map<String, Object> config
    ) {
        public PrepStepDto {
            config = config == null ? Map.of() : Map.copyOf(config);
        }
    }

    public record LineageStepDto(
            int stepIndex,
            String stepType,
            String description,
            long inputRows,
            long outputRows
    ) {
    }
}
