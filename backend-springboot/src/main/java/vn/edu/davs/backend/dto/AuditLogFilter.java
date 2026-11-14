package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Filter criteria for querying {@code audit_logs}.
 */
public record AuditLogFilter(
        UUID actorId,
        String actorEmail,
        String action,
        String resourceType,
        Instant from,
        Instant to
) {
}
