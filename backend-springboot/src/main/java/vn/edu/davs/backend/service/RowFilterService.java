package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.JdbcRowFilterRepository;
import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.DatasourceRlsStatusDto;
import vn.edu.davs.backend.dto.RowFilterDto;
import vn.edu.davs.backend.dto.SaveRowFilterRequest;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.model.DavsUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Row-Level Security (RLS) policy management and runtime enforcement service (T-101).
 */
@Service
@RequiredArgsConstructor
public class RowFilterService implements RowFilterProvider {

    private final JdbcRowFilterRepository repository;
    private final PermissionService permissionService;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public boolean isRlsEnabled(UUID datasourceId) {
        return repository.isRlsEnabled(datasourceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> resolveRowFilters(UUID datasourceId, DavsUser actor) {
        if (datasourceId == null || actor == null) {
            return Map.of();
        }
        if (!repository.isRlsEnabled(datasourceId)) {
            return Map.of();
        }
        return repository.findFiltersForUser(datasourceId, actor.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> applyRowFilters(
            UUID datasourceId,
            DavsUser actor,
            List<Map<String, Object>> rows
    ) {
        if (rows == null || rows.isEmpty() || datasourceId == null) {
            return rows == null ? List.of() : rows;
        }
        if (!repository.isRlsEnabled(datasourceId)) {
            return rows;
        }
        if (actor != null && actor.getSiteRole() == SiteRole.SITE_ADMIN) {
            return rows;
        }
        if (actor == null) {
            return List.of();
        }
        Map<String, List<String>> filters = repository.findFiltersForUser(datasourceId, actor.getId());
        if (filters.isEmpty()) {
            // BUS-VIZ-15 / Default-deny when RLS is enabled on datasource but user has no mapping
            return List.of();
        }
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (matchesAllFilters(row, filters)) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    @Override
    @Transactional(readOnly = true)
    public String buildSqlWhereClause(
            UUID datasourceId,
            DavsUser actor,
            Map<String, String> allowedFieldMapping,
            List<Object> sqlParams
    ) {
        if (datasourceId == null || !repository.isRlsEnabled(datasourceId)) {
            return "";
        }
        if (actor != null && actor.getSiteRole() == SiteRole.SITE_ADMIN) {
            return "";
        }
        if (actor == null) {
            return "1 = 0";
        }
        Map<String, List<String>> filters = repository.findFiltersForUser(datasourceId, actor.getId());
        if (filters.isEmpty()) {
            return "1 = 0";
        }
        List<String> predicates = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
            String safeColumn = resolveSafeColumn(entry.getKey(), allowedFieldMapping);
            List<String> values = entry.getValue();
            if (safeColumn == null || values == null || values.isEmpty()) {
                return "1 = 0";
            }
            String placeholders = String.join(", ", values.stream().map(v -> "?").toList());
            predicates.add(safeColumn + " IN (" + placeholders + ")");
            sqlParams.addAll(values);
        }
        return String.join(" AND ", predicates);
    }

    @Transactional(readOnly = true)
    public List<DatasourceRlsStatusDto> listDatasourceRlsStatuses(DavsUser actor) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return repository.listDatasourceRlsStatuses();
    }

    @Transactional
    public DatasourceRlsStatusDto toggleDatasourceRls(UUID datasourceId, boolean enabled, DavsUser actor) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        boolean updated = repository.setRlsEnabled(datasourceId, enabled);
        if (!updated) {
            throw new DavsException(ErrorCode.ERR_PRJ_02, "Không tìm thấy nguồn dữ liệu");
        }
        auditService.log(AuditEvent.of(
                "RLS_DATASOURCE_TOGGLED",
                actor.getId(),
                actor.getEmail(),
                "DATASOURCE",
                datasourceId.toString()
        ).withStates(null, Map.of("rlsEnabled", enabled)));
        return repository.listDatasourceRlsStatuses().stream()
                .filter(item -> item.datasourceId().equals(datasourceId))
                .findFirst()
                .orElse(new DatasourceRlsStatusDto(datasourceId, "", enabled));
    }

    @Transactional(readOnly = true)
    public List<RowFilterDto> listRowFilters(UUID datasourceId, DavsUser actor) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return repository.listRowFilters(datasourceId);
    }

    @Transactional
    public RowFilterDto saveRowFilter(SaveRowFilterRequest request, DavsUser actor) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        RowFilterDto saved = repository.upsertRowFilter(request, actor.getId());
        auditService.log(AuditEvent.of(
                "RLS_POLICY_SAVED",
                actor.getId(),
                actor.getEmail(),
                "ROW_FILTER",
                saved != null ? saved.id().toString() : request.datasourceId().toString()
        ).withStates(null, Map.of(
                "datasourceId", request.datasourceId().toString(),
                "subjectType", request.subjectType(),
                "subjectId", request.subjectId().toString(),
                "fieldName", request.fieldName(),
                "allowedValues", request.allowedValues()
        )));
        return saved;
    }

    @Transactional
    public void deleteRowFilter(UUID id, DavsUser actor) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        if (!repository.deleteRowFilter(id)) {
            throw new DavsException(ErrorCode.ERR_PRJ_02, "Không tìm thấy cấu hình bảo mật dòng");
        }
        auditService.log(AuditEvent.of(
                "RLS_POLICY_DELETED",
                actor.getId(),
                actor.getEmail(),
                "ROW_FILTER",
                id.toString()
        ));
    }

    private boolean matchesAllFilters(Map<String, Object> row, Map<String, List<String>> filters) {
        for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
            String field = entry.getKey();
            List<String> allowed = entry.getValue();
            if (allowed == null || allowed.isEmpty()) {
                return false;
            }
            Object rowVal = extractFieldValueCaseInsensitive(row, field);
            if (rowVal == null) {
                return false;
            }
            String actualStr = String.valueOf(rowVal).trim();
            boolean match = allowed.stream().anyMatch(v -> v.trim().equalsIgnoreCase(actualStr));
            if (!match) {
                return false;
            }
        }
        return true;
    }

    private Object extractFieldValueCaseInsensitive(Map<String, Object> row, String fieldName) {
        if (row.containsKey(fieldName)) {
            return row.get(fieldName);
        }
        String target = fieldName.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = entry.getKey().trim().toLowerCase(Locale.ROOT);
            if (key.equals(target) || (target.equals("vung") && key.equals("region"))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String resolveSafeColumn(String fieldName, Map<String, String> allowedFieldMapping) {
        if (allowedFieldMapping == null || allowedFieldMapping.isEmpty()) {
            return null;
        }
        if (allowedFieldMapping.containsKey(fieldName)) {
            return allowedFieldMapping.get(fieldName);
        }
        String target = fieldName.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : allowedFieldMapping.entrySet()) {
            if (entry.getKey().trim().toLowerCase(Locale.ROOT).equals(target)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
