package com.davs.audit;

import java.util.Objects;
import java.util.UUID;

/**
 * Domain event passed to {@link AuditService#log(AuditEvent)}.
 */
public record AuditEvent(
        String action,
        UUID actorId,
        String actorEmail,
        String resourceType,
        String resourceId,
        Object beforeState,
        Object afterState,
        String ipAddress,
        String userAgent,
        String traceId
) {
    public AuditEvent {
        Objects.requireNonNull(action, "action must not be null");
        if (action.isBlank()) {
            throw new IllegalArgumentException("action must not be blank");
        }
    }

    public static AuditEvent of(String action, String resourceType, String resourceId) {
        return new AuditEvent(action, null, null, resourceType, resourceId, null, null, null, null, null);
    }

    public static AuditEvent of(
            String action,
            UUID actorId,
            String actorEmail,
            String resourceType,
            String resourceId
    ) {
        return new AuditEvent(action, actorId, actorEmail, resourceType, resourceId, null, null, null, null, null);
    }

    public AuditEvent withStates(Object newBeforeState, Object newAfterState) {
        return new AuditEvent(
                action,
                actorId,
                actorEmail,
                resourceType,
                resourceId,
                newBeforeState,
                newAfterState,
                ipAddress,
                userAgent,
                traceId
        );
    }

    public AuditEvent withRequestContext(String ip, String ua, String trace) {
        return new AuditEvent(
                action,
                actorId,
                actorEmail,
                resourceType,
                resourceId,
                beforeState,
                afterState,
                ip,
                ua,
                trace
        );
    }
}
