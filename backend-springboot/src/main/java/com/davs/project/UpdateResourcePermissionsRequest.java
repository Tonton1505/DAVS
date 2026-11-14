package com.davs.project;

import com.davs.permission.Capability;
import com.davs.permission.Effect;
import com.davs.permission.SubjectType;
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
