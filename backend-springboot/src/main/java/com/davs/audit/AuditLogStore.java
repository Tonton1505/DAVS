package com.davs.audit;

import com.davs.common.api.PageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/**
 * Persistence abstraction for append-only {@code audit_logs}.
 */
public interface AuditLogStore {

    void insert(
            String action,
            UUID actorId,
            String actorEmail,
            String resourceType,
            String resourceId,
            JsonNode beforeState,
            JsonNode afterState,
            String ipAddress,
            String userAgent,
            String traceId
    );

    PageResponse<AuditLogResponse> search(AuditLogFilter filter, Pageable pageable);
}
