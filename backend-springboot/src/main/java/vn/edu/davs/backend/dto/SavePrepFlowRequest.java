package vn.edu.davs.backend.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request body for creating or updating a data prep flow (UC12).
 */
public record SavePrepFlowRequest(
        UUID datasourceId,
        String name,
        String description,
        List<PrepFlowDto.PrepStepDto> steps,
        Boolean published
) {
}
