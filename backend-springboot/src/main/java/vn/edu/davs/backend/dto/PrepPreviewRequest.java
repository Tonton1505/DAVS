package vn.edu.davs.backend.dto;

import java.util.List;

/**
 * Request body for {@code POST /api/v1/prep-flows/{id}/preview} (UC12).
 */
public record PrepPreviewRequest(
        Integer stepIndex,
        List<PrepFlowDto.PrepStepDto> steps
) {
}
