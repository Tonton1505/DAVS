package vn.edu.davs.backend.perf;




import vn.edu.davs.backend.service.impl.DashboardServiceImpl;
import vn.edu.davs.backend.service.impl.ExportServiceImpl;
import vn.edu.davs.backend.service.impl.QueryServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.davs.backend.dao.IDashboardDao;
import vn.edu.davs.backend.dao.IWorkbookDao;
import vn.edu.davs.backend.dto.DashboardDto;
import vn.edu.davs.backend.dto.ExportRequest;
import vn.edu.davs.backend.dto.QueryRequest;
import vn.edu.davs.backend.dto.QueryResult;
import vn.edu.davs.backend.dto.QuerySpec;
import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.model.ResourceRef;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.service.DashboardService;
import vn.edu.davs.backend.service.ExportService;
import vn.edu.davs.backend.service.PermissionService;
import vn.edu.davs.backend.service.QueryService;
import vn.edu.davs.backend.service.RowFilterProvider;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-103 Performance & Caching Benchmarks for QueryService, DashboardService, and ExportService.
 */
class PerformanceBenchmarkTests {

    @Test
    void queryServiceCachesResultsPerQuerySpecAndActorWithin3Seconds() {
        RowFilterProvider rls = mock(RowFilterProvider.class);
        UUID dsId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        when(rls.isRlsEnabled(dsId)).thenReturn(false);

        QueryService queryService = new QueryServiceImpl(rls);
        DavsUser actor = DavsUser.of(UUID.randomUUID(), "explorer@davs.vn", "Explorer", SiteRole.EXPLORER);
        QuerySpec spec = new QuerySpec(
                dsId,
                List.of("Tháng (Ngày đặt hàng)"),
                List.of("SUM(Doanh số)"),
                Map.of(),
                List.of(),
                List.of(),
                12
        );

        long startMs = System.currentTimeMillis();
        QueryResult first = queryService.executeQuery(spec, actor);
        for (int i = 0; i < 500; i++) {
            QueryResult cached = queryService.executeQuery(spec, actor);
            assertThat(cached).isSameAs(first);
        }
        long elapsedMs = System.currentTimeMillis() - startMs;

        assertThat(elapsedMs).isLessThan(3_000L);
    }

    @Test
    void dashboardInteractiveQueryAndCsvExportMeetLatencyTargets() {
        IDashboardDao dashRepo = mock(IDashboardDao.class);
        IWorkbookDao wbRepo = mock(IWorkbookDao.class);
        PermissionService perm = mock(PermissionService.class);
        AuditService audit = mock(AuditService.class);

        UUID dashId = UUID.randomUUID();
        when(perm.can(any(), eq(Capability.VIEW), any(ResourceRef.class))).thenReturn(true);
        when(perm.can(any(), eq(Capability.DOWNLOAD), any(ResourceRef.class))).thenReturn(true);
        when(dashRepo.findById(dashId)).thenReturn(Optional.of(new DashboardDto(
                dashId, UUID.randomUUID(), "Perf Dashboard", null, "{}", null, Instant.now(), Instant.now()
        )));

        DashboardService dashboardService = new DashboardServiceImpl(
                dashRepo, wbRepo, perm, audit, new ObjectMapper()
        );
        ExportService exportService = new ExportServiceImpl(dashRepo, perm, audit);
        DavsUser viewer = DavsUser.of(UUID.randomUUID(), "viewer@davs.vn", "Viewer", SiteRole.VIEWER);

        long startQuery = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            dashboardService.executeQuery(
                    new QueryRequest(dashId, null, Map.of("region", "Miền Bắc"), Map.of(), null),
                    viewer
            );
        }
        long queryElapsed = System.currentTimeMillis() - startQuery;
        assertThat(queryElapsed).isLessThan(3_000L);

        long startExport = System.currentTimeMillis();
        ExportService.ExportResult csv = exportService.export(
                new ExportRequest(dashId, null, "CSV", "full_data", null, null, Map.of()),
                viewer
        );
        long exportElapsed = System.currentTimeMillis() - startExport;
        assertThat(csv.data()).isNotEmpty();
        assertThat(exportElapsed).isLessThan(3_000L);
    }
}
