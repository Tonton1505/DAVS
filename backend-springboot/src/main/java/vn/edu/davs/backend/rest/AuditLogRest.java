package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.AuditLogResponse;
import vn.edu.davs.backend.dto.PageResponse;
import vn.edu.davs.backend.model.DavsUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin-only REST endpoint for querying immutable audit logs (T-007).
 */
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit Logs", description = "Tra cứu nhật ký kiểm toán (chỉ SITE_ADMIN)")
public interface AuditLogRest {


    @GetMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    @Operation(summary = "Danh sách nhật ký kiểm toán có lọc và phân trang")
    PageResponse<AuditLogResponse> listAuditLogs(
            @AuthenticationPrincipal DavsUser currentUser,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    );
}
