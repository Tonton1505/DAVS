package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.AuditLogStore;
import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.AuditLogFilter;
import vn.edu.davs.backend.dto.AuditLogResponse;

import vn.edu.davs.backend.dto.PageResponse;
import vn.edu.davs.backend.model.DavsUser;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Append-only audit logging service (T-007).
 * Writes inside the caller's transaction ({@link Propagation#REQUIRED}) and masks sensitive fields.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final int MAX_IP_LENGTH = 64;
    private static final int MAX_UA_LENGTH = 500;
    private static final int MAX_TRACE_LENGTH = 64;

    private final AuditLogStore auditLogStore;
    private final SensitiveDataMasker sensitiveDataMasker;

    /**
     * Persists an audit event in the current transaction after redacting sensitive fields.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void log(AuditEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        DavsUser principal = currentPrincipal();
        UUID actorId = event.actorId() != null ? event.actorId() : (principal != null ? principal.getId() : null);
        String actorEmail = event.actorEmail() != null
                ? event.actorEmail()
                : (principal != null ? principal.getEmail() : null);

        HttpServletRequest currentRequest = currentHttpRequest();
        String ipAddress = truncate(
                event.ipAddress() != null ? event.ipAddress() : resolveIp(currentRequest),
                MAX_IP_LENGTH
        );
        String userAgent = truncate(
                event.userAgent() != null ? event.userAgent() : resolveUserAgent(currentRequest),
                MAX_UA_LENGTH
        );
        String traceId = truncate(
                event.traceId() != null ? event.traceId() : resolveTraceId(currentRequest),
                MAX_TRACE_LENGTH
        );

        JsonNode maskedBefore = sensitiveDataMasker.mask(event.beforeState());
        JsonNode maskedAfter = sensitiveDataMasker.mask(event.afterState());

        auditLogStore.insert(
                event.action(),
                actorId,
                actorEmail,
                event.resourceType(),
                event.resourceId(),
                maskedBefore,
                maskedAfter,
                ipAddress,
                userAgent,
                traceId
        );
    }

    /**
     * Queries audit logs using optional filters and pagination.
     */
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(AuditLogFilter filter, Pageable pageable) {
        return auditLogStore.search(filter, pageable);
    }

    private static DavsUser currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof DavsUser user) {
            return user;
        }
        return null;
    }

    private static HttpServletRequest currentHttpRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private static String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private static String resolveUserAgent(HttpServletRequest request) {
        return request != null ? request.getHeader(HttpHeaders.USER_AGENT) : null;
    }

    private static String resolveTraceId(HttpServletRequest request) {
        return request != null ? request.getHeader("X-Request-Id") : null;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
