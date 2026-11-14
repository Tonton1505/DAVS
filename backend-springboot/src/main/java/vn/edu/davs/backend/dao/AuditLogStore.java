package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.AuditLogFilter;
import vn.edu.davs.backend.dto.AuditLogResponse;

import vn.edu.davs.backend.dto.PageResponse;
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
