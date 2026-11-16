package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/labels/{id}/assign} and {@code /unassign} (UC09 [6a]).
 */
public record LabelTargetRequest(
        @NotEmpty(message = "Danh sách đối tượng gán nhãn không được để trống")
        List<TargetRef> targets
) {
    public record TargetRef(
            String type,
            UUID id
    ) {
    }
}
