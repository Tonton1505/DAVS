package vn.edu.davs.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.davs.backend.dao.DashboardDao;
import vn.edu.davs.backend.dao.JdbcRowFilterRepository;
import vn.edu.davs.backend.dao.WorkbookDao;
import vn.edu.davs.backend.dto.DashboardDto;
import vn.edu.davs.backend.dto.ExportRequest;
import vn.edu.davs.backend.dto.QueryRequest;
import vn.edu.davs.backend.dto.QueryResult;
import vn.edu.davs.backend.dto.QueryResultDto;
import vn.edu.davs.backend.dto.QuerySpec;
import vn.edu.davs.backend.dto.SaveRowFilterRequest;
import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.model.ResourceRef;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.service.DashboardService;
import vn.edu.davs.backend.service.ExportService;
import vn.edu.davs.backend.service.PermissionService;
import vn.edu.davs.backend.service.QueryService;
import vn.edu.davs.backend.service.RowFilterService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for T-101 Row-Level Security (RLS) across Query, Dashboard View, and Export.
 */
class RowFilterSecurityTests {

    private static final UUID SAMPLE_DS_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");

    private JdbcRowFilterRepository rowFilterRepository;
    private PermissionService permissionService;
    private AuditService auditService;
    private RowFilterService rowFilterService;

    private DavsUser northUser;
    private DavsUser southUser;
    private DavsUser unmappedUser;

    @BeforeEach
    void setUp() {
        rowFilterRepository = mock(JdbcRowFilterRepository.class);
        permissionService = mock(PermissionService.class);
        auditService = mock(AuditService.class);
        rowFilterService = new RowFilterService(rowFilterRepository, permissionService, auditService);

        northUser = DavsUser.of(UUID.randomUUID(), "north@davs.vn", "North Viewer", SiteRole.VIEWER);
        southUser = DavsUser.of(UUID.randomUUID(), "south@davs.vn", "South Viewer", SiteRole.VIEWER);
        unmappedUser = DavsUser.of(UUID.randomUUID(), "unmapped@davs.vn", "Unmapped Viewer", SiteRole.VIEWER);

        when(rowFilterRepository.isRlsEnabled(SAMPLE_DS_ID)).thenReturn(true);
        when(rowFilterRepository.findFiltersForUser(SAMPLE_DS_ID, northUser.getId()))
                .thenReturn(Map.of("Vung", List.of("Miền Bắc")));
        when(rowFilterRepository.findFiltersForUser(SAMPLE_DS_ID, southUser.getId()))
                .thenReturn(Map.of("Vung", List.of("Miền Nam")));
        when(rowFilterRepository.findFiltersForUser(SAMPLE_DS_ID, unmappedUser.getId()))
                .thenReturn(Map.of());
    }

    @Test
    void twoUsersWithDifferentMappingsSeeDifferentDatasetsInQueryDashboardAndExport() {
        QueryService queryService = new QueryService(rowFilterService);
        QuerySpec spec = new QuerySpec(
                SAMPLE_DS_ID,
                List.of("Vung"),
                List.of("Doanh_so"),
                Map.of(),
                List.of(),
                List.of(),
                12
        );

        QueryResult northQuery = queryService.executeQuery(spec, northUser);
        QueryResult southQuery = queryService.executeQuery(spec, southUser);

        assertThat(northQuery.rows()).isNotEmpty();
        assertThat(northQuery.rows()).allMatch(r -> "Miền Bắc".equals(r.get("Vung")));
        assertThat(southQuery.rows()).isNotEmpty();
        assertThat(southQuery.rows()).allMatch(r -> "Miền Nam".equals(r.get("Vung")));

        // Dashboard View query enforcement
        DashboardDao dashRepo = mock(DashboardDao.class);
        WorkbookDao wbRepo = mock(WorkbookDao.class);
        DashboardService dashboardService = new DashboardService(
                dashRepo, wbRepo, permissionService, auditService, new ObjectMapper(), rowFilterService
        );
        UUID dashId = UUID.randomUUID();
        when(permissionService.can(any(), eq(Capability.VIEW), any(ResourceRef.class))).thenReturn(true);

        QueryResultDto northDash = dashboardService.executeQuery(
                new QueryRequest(dashId, null, Map.of("region", "Tất cả vùng"), Map.of(), null),
                northUser
        );
        QueryResultDto southDash = dashboardService.executeQuery(
                new QueryRequest(dashId, null, Map.of("region", "Tất cả vùng"), Map.of(), null),
                southUser
        );

        assertThat(northDash.data()).isNotEmpty();
        assertThat(northDash.data()).allMatch(r -> "Miền Bắc".equals(r.get("Vung")));
        assertThat(southDash.data()).isNotEmpty();
        assertThat(southDash.data()).allMatch(r -> "Miền Nam".equals(r.get("Vung")));

        // Export CSV enforcement
        ExportService exportService = new ExportService(dashRepo, permissionService, auditService, rowFilterService);
        DashboardDto dashDto = new DashboardDto(
                dashId, UUID.randomUUID(), "Doanh số 2026", null, "{}", null, Instant.now(), Instant.now()
        );
        when(dashRepo.findById(dashId)).thenReturn(Optional.of(dashDto));
        when(permissionService.can(any(), eq(Capability.DOWNLOAD), any(ResourceRef.class))).thenReturn(true);

        ExportService.ExportResult northCsv = exportService.export(
                new ExportRequest(dashId, null, "CSV", "full_data", null, null, Map.of()),
                northUser
        );
        ExportService.ExportResult southCsv = exportService.export(
                new ExportRequest(dashId, null, "CSV", "full_data", null, null, Map.of()),
                southUser
        );

        String northText = new String(northCsv.data(), StandardCharsets.UTF_8);
        String southText = new String(southCsv.data(), StandardCharsets.UTF_8);
        assertThat(northText).contains("Miền Bắc").doesNotContain("Miền Nam");
        assertThat(southText).contains("Miền Nam").doesNotContain("Miền Bắc");
    }

    @Test
    void craftedQuerySpecCannotBypassRlsAndUnmappedUserGetsNoRows() {
        QueryService queryService = new QueryService(rowFilterService);
        // Attack attempt: northUser sends crafted filters asking for Miền Nam or bypass flag
        QuerySpec craftedAttackSpec = new QuerySpec(
                SAMPLE_DS_ID,
                List.of("Vung"),
                List.of("Doanh_so"),
                Map.of(),
                List.of(Map.of("field", "Vung", "op", "IN", "values", List.of("Miền Nam"))),
                List.of(),
                12
        );

        QueryResult attackResult = queryService.executeQuery(craftedAttackSpec, northUser);
        assertThat(attackResult.rows()).noneMatch(r -> "Miền Nam".equals(r.get("Vung")));
        assertThat(attackResult.rows()).allMatch(r -> "Miền Bắc".equals(r.get("Vung")));

        // Default-deny when RLS is enabled on datasource but user has no mapping
        QueryResult unmappedResult = queryService.executeQuery(craftedAttackSpec, unmappedUser);
        assertThat(unmappedResult.rows()).isEmpty();
    }

    @Test
    void buildSqlWhereClauseUsesAllowlistedColumnsAndParameterizedValues() {
        List<Object> params = new ArrayList<>();
        String where = rowFilterService.buildSqlWhereClause(
                SAMPLE_DS_ID,
                northUser,
                Map.of("Vung", "vung"),
                params
        );

        assertThat(where).isEqualTo("vung IN (?)");
        assertThat(params).containsExactly("Miền Bắc");

        // Non-allowlisted field -> fail closed ("1 = 0") to prevent SQL injection
        List<Object> injectedParams = new ArrayList<>();
        String failClosed = rowFilterService.buildSqlWhereClause(
                SAMPLE_DS_ID,
                northUser,
                Map.of("other_col", "other_col"),
                injectedParams
        );
        assertThat(failClosed).isEqualTo("1 = 0");
        assertThat(injectedParams).isEmpty();
    }

    @Test
    void nonAdminCannotModifyRlsPolicies() {
        doThrow(new DavsException(ErrorCode.ERR_PRJ_03))
                .when(permissionService).requireRole(northUser, SiteRole.SITE_ADMIN);

        SaveRowFilterRequest req = new SaveRowFilterRequest(
                SAMPLE_DS_ID, "USER", northUser.getId(), "Vung", List.of("Miền Bắc")
        );

        assertThatThrownBy(() -> rowFilterService.saveRowFilter(req, northUser))
                .isInstanceOf(DavsException.class)
                .extracting(e -> ((DavsException) e).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_03);
    }
}
