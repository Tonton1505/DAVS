package vn.edu.davs.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.PublishWorkbookRequest;
import vn.edu.davs.backend.dto.PublishWorkbookResponse;
import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.common.exception.DavsException;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.model.ResourceRef;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.davs.backend.service.PublishService;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.service.PermissionService;

@RequiredArgsConstructor
@Slf4j
@Service
public class PublishServiceImpl implements PublishService {


    private final JdbcTemplate jdbcTemplate;

    private final PermissionService permissionService;

    private final AuditService auditService;

    private final ObjectMapper objectMapper;


    /**
     * Publishes a workbook to Internal Server, Cloud, or Public destination.
     */
    @Transactional
    @Override
    public PublishWorkbookResponse publishWorkbook(
            UUID workbookId,
            PublishWorkbookRequest request,
            DavsUser actor
    ) {
        Objects.requireNonNull(workbookId, "workbookId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        // 1. Verify source workbook exists
        Map<String, Object> sourceWorkbook = getWorkbookRow(workbookId);
        if (sourceWorkbook == null) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }

        UUID targetProjectId = request.projectId() != null
                ? request.projectId()
                : (UUID) sourceWorkbook.get("project_id");

        // 2. BUS-SHR-01: Check permission at target project
        boolean hasPublishPermission = actor.getSiteRole().isAtLeast(SiteRole.CREATOR)
                && (actor.getSiteRole() == SiteRole.SITE_ADMIN
                || permissionService.can(actor, Capability.PUBLISH, ResourceRef.project(targetProjectId))
                || isProjectOwnerOrEditor(targetProjectId, actor.getId()));

        if (!hasPublishPermission) {
            throw new DavsException(ErrorCode.ERR_PUB_01);
        }

        String destination = (request.destination() != null && !request.destination().isBlank())
                ? request.destination().trim().toUpperCase()
                : "INTERNAL_SERVER";

        // 3. BUS-SHR-03: If destination is Public, forbid publishing if content has Confidential / Bảo mật label
        if ("PUBLIC".equalsIgnoreCase(destination)) {
            if (hasConfidentialLabel(workbookId)) {
                throw new DavsException(ErrorCode.ERR_PUB_01);
            }
        }

        // 4. ERR-PUB-03: Check if embedded data source connection is broken
        Boolean needsReconnect = (Boolean) sourceWorkbook.get("needs_reconnect");
        if (Boolean.TRUE.equals(needsReconnect)) {
            throw new DavsException(ErrorCode.ERR_PUB_03);
        }

        String publishName = (request.name() != null && !request.name().trim().isBlank())
                ? request.name().trim()
                : (String) sourceWorkbook.get("name");

        // 5. ERR-PUB-02: Check name collision in target project
        UUID existingWorkbookId = findWorkbookIdByProjectAndName(targetProjectId, publishName);
        if (existingWorkbookId != null && !existingWorkbookId.equals(workbookId)) {
            if (!request.overwrite()) {
                throw new DavsException(ErrorCode.ERR_PUB_02);
            }
            // Overwrite existing: increment version
            incrementWorkbookVersion(existingWorkbookId);
        }

        // 6. BUS-SHR-02: Check extract without schedule warning
        List<String> warnings = new ArrayList<>();
        String dataSourceMode = request.dataSourceMode() != null
                ? request.dataSourceMode().trim().toUpperCase()
                : "EMBEDDED";

        if ("EXTRACT".equalsIgnoreCase(dataSourceMode) || hasExtractWithoutSchedule(workbookId)) {
            warnings.add("Workbook dùng Extract cần cấu hình lịch refresh hoặc cảnh báo dữ liệu tĩnh.");
        }

        // 7. Insert published record
        UUID publishId = UUID.randomUUID();
        String visibleSheetsJson = writeJson(request.visibleSheets() != null ? request.visibleSheets() : List.of());
        String publicUrl = buildPublishUrl(destination, targetProjectId, workbookId, publishName);
        int currentVer = (Integer) sourceWorkbook.getOrDefault("current_version", 1);

        String insertSql = """
                INSERT INTO published_workbooks (
                    id, workbook_id, project_id, publisher_id, destination, published_name,
                    description, visible_sheets, data_source_mode, is_packaged_twbx,
                    has_embedded_credentials, public_url, version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, now(), now())
                """;

        jdbcTemplate.update(
                insertSql,
                publishId,
                workbookId,
                targetProjectId,
                actor.getId(),
                destination,
                publishName,
                request.description(),
                visibleSheetsJson,
                dataSourceMode,
                request.packagedTwbx(),
                request.hasEmbeddedCredentials(),
                publicUrl,
                currentVer
        );

        // 8. Audit log
        auditService.log(AuditEvent.of(
                "WORKBOOK_PUBLISHED",
                actor.getId(),
                actor.getEmail(),
                "WORKBOOK",
                workbookId.toString()
        ));

        return new PublishWorkbookResponse(
                publishId,
                workbookId,
                targetProjectId,
                destination,
                publishName,
                publicUrl,
                currentVer,
                warnings,
                Instant.now()
        );
    }


    /**
     * Lists available destination targets and default project info for publishing.
     */
    @Override
    public Map<String, Object> getPublishTargets(UUID workbookId, DavsUser actor) {
        Map<String, Object> wb = getWorkbookRow(workbookId);
        if (wb == null) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }

        List<Map<String, Object>> projects = jdbcTemplate.queryForList("""
                SELECT id, name, description FROM projects
                WHERE deleted_at IS NULL
                ORDER BY name ASC
                """);

        return Map.of(
                "workbookId", workbookId,
                "currentName", wb.get("name"),
                "currentProjectId", wb.get("project_id"),
                "projects", projects,
                "destinations", List.of("INTERNAL_SERVER", "CLOUD", "PUBLIC")
        );
    }


    private Map<String, Object> getWorkbookRow(UUID workbookId) {
        try {
            return jdbcTemplate.queryForMap("""
                    SELECT id, project_id, name, description, current_version, owner_id,
                           needs_reconnect, created_at, updated_at
                    FROM workbooks
                    WHERE id = ? AND deleted_at IS NULL
                    """, workbookId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }


    private UUID findWorkbookIdByProjectAndName(UUID projectId, String name) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT id FROM workbooks
                    WHERE project_id = ? AND lower(name) = lower(?) AND deleted_at IS NULL
                    LIMIT 1
                    """, UUID.class, projectId, name);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }


    private void incrementWorkbookVersion(UUID workbookId) {
        jdbcTemplate.update("""
                UPDATE workbooks
                SET current_version = current_version + 1, updated_at = now()
                WHERE id = ?
                """, workbookId);
    }


    private boolean isProjectOwnerOrEditor(UUID projectId, UUID userId) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM projects
                    WHERE id = ? AND (owner_id = ? OR created_by = ?) AND deleted_at IS NULL
                    """, Integer.class, projectId, userId, userId);
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }


    private boolean hasConfidentialLabel(UUID workbookId) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT count(*)
                    FROM label_assignments la
                    JOIN labels l ON la.label_id = l.id
                    WHERE la.target_type = 'WORKBOOK'
                      AND la.target_id = ?
                      AND (
                          lower(l.value) LIKE '%bảo mật%'
                          OR lower(l.value) LIKE '%confidential%'
                          OR lower(l.value) LIKE '%mật%'
                          OR lower(l.value) LIKE '%internal%'
                      )
                    """, Integer.class, workbookId);
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }


    private boolean hasExtractWithoutSchedule(UUID workbookId) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT count(*)
                    FROM datasources d
                    WHERE d.storage_mode = 'EXTRACT'
                      AND d.refresh_cron IS NULL
                      AND d.id IN (
                          SELECT datasource_id FROM workbook_datasources WHERE workbook_id = ?
                      )
                    """, Integer.class, workbookId);
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }


    private String buildPublishUrl(String dest, UUID projectId, UUID workbookId, String name) {
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (slug.isBlank()) {
            slug = workbookId.toString().substring(0, 8);
        }
        return switch (dest.toUpperCase()) {
            case "PUBLIC" -> "https://davs.public.vn/view/" + slug;
            case "CLOUD" -> "https://app.davs-cloud.com/d/" + slug;
            default -> "/dashboards/" + slug;
        };
    }


    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
