package vn.edu.davs.backend.dto;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Request payload for saving interactive actions on a dashboard (UC18).
 */
public record SaveActionsRequest(
        @Valid
        List<DashboardActionDto> actions
) {
    public SaveActionsRequest {
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
