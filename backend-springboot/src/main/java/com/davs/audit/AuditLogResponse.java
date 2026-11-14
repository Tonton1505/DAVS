package com.davs.audit;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/**
 * API representation of a single audit log entry returned by {@code GET /api/v1/audit-logs}.
 */
public record AuditLogResponse(
        UUID id,
        String action,
        UUID actorId,
        String actorEmail,
        String resourceType,
        String resourceId,
        JsonNode beforeState,
        JsonNode afterState,
        String ipAddress,
        String userAgent,
        String traceId,
        Instant createdAt
) {
}
