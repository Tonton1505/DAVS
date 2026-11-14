package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Background prep flow run DTO returned by {@code /api/v1/prep-flows/{id}/run} and {@code /runs} (UC12).
 */
public record PrepRunDto(
        UUID runId,
        UUID flowId,
        UUID jobId,
        String status,
        long inputRows,
        long outputRows,
        long removedDuplicates,
        long replacedNulls,
        List<PrepFlowDto.LineageStepDto> lineage,
        String errorCode,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {
    public PrepRunDto {
        lineage = lineage == null ? List.of() : List.copyOf(lineage);
    }
}
