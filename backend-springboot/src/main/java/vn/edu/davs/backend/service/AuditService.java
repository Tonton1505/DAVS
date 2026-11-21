package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.AuditLogFilter;
import vn.edu.davs.backend.dto.AuditLogResponse;
import vn.edu.davs.backend.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Append-only audit logging service (T-007).
 * Writes inside the caller's transaction ({@link Propagation#REQUIRED}) and masks sensitive fields.
 */
public interface AuditService {


    /**
     * Persists an audit event in the current transaction after redacting sensitive fields.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    void log(AuditEvent event);


    /**
     * Queries audit logs using optional filters and pagination.
     */
    @Transactional(readOnly = true)
    PageResponse<AuditLogResponse> search(AuditLogFilter filter, Pageable pageable);
}
