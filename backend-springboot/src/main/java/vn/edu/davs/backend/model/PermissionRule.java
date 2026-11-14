package vn.edu.davs.backend.model;

import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.Effect;
import vn.edu.davs.backend.enu.ResourceType;
import vn.edu.davs.backend.enu.SubjectType;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single row in {@code permission_rules}.
 */
public record PermissionRule(
        UUID id,
        SubjectType subjectType,
        UUID subjectId,
        ResourceType resourceType,
        UUID resourceId,
        Capability capability,
        Effect effect
) {
    public PermissionRule {
        Objects.requireNonNull(subjectType, "subjectType must not be null");
        Objects.requireNonNull(subjectId, "subjectId must not be null");
        Objects.requireNonNull(resourceType, "resourceType must not be null");
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        Objects.requireNonNull(capability, "capability must not be null");
        Objects.requireNonNull(effect, "effect must not be null");
    }

    public static PermissionRule of(
            SubjectType subjectType,
            UUID subjectId,
            ResourceRef resource,
            Capability capability,
            Effect effect
    ) {
        return new PermissionRule(
                null,
                subjectType,
                subjectId,
                resource.type(),
                resource.id(),
                capability,
                effect
        );
    }
}
