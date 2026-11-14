package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.Effect;
import vn.edu.davs.backend.enu.SubjectType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Request payload for {@code PUT /api/v1/resources/{type}/{id}/permissions}.
 */
public record UpdateResourcePermissionsRequest(
        @NotNull @Valid List<RuleInput> rules
) {
    /**
     * Individual rule input item.
     */
    public record RuleInput(
            @NotNull SubjectType subjectType,
            @NotNull UUID subjectId,
            @NotNull Capability capability,
            @NotNull Effect effect
    ) {
    }
}
