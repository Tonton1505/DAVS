package vn.edu.davs.backend.rest.impl;

import vn.edu.davs.backend.dto.AuditLogFilter;
import vn.edu.davs.backend.dto.AuditLogResponse;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.dto.PageResponse;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.service.PermissionService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.rest.AuditLogRest;

@RequiredArgsConstructor
@RestController
public class AuditLogRestImpl implements AuditLogRest {


    private static final int MAX_PAGE_SIZE = 100;


    private final AuditService auditService;

    private final PermissionService permissionService;

    @Override
    public PageResponse<AuditLogResponse> listAuditLogs(
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
    ) {
        permissionService.requireRole(currentUser, SiteRole.SITE_ADMIN);

        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        Pageable pageable = PageRequest.of(safePage, safeSize, parseSort(sort));

        AuditLogFilter filter = new AuditLogFilter(actorId, actorEmail, action, resourceType, from, to);
        return auditService.search(filter, pageable);
    }


    private static Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sortParam.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}
