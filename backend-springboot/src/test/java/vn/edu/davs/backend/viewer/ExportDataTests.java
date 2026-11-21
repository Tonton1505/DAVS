package vn.edu.davs.backend.viewer;

import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.dao.DashboardDao;
import vn.edu.davs.backend.dto.DashboardDto;
import vn.edu.davs.backend.dto.ExportLimitsDto;
import vn.edu.davs.backend.dto.ExportRequest;
import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.model.ResourceRef;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.service.ExportService;
import vn.edu.davs.backend.service.PermissionService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportDataTests {

    @Mock
    private DashboardDao dashboardRepository;
    @Mock
    private PermissionService permissionService;
    @Mock
    private AuditService auditService;

    private ExportService exportService;

    private final DavsUser viewer = DavsUser.of(UUID.randomUUID(), "viewer@davs.vn", "Viewer User", SiteRole.VIEWER);
    private final UUID dashboardId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        exportService = new ExportService(dashboardRepository, permissionService, auditService);
    }

    @Test
    void exportLimitsReturnsStandard500kRows() {
        ExportLimitsDto limits = exportService.getLimits();
        assertThat(limits.maxRows()).isEqualTo(500_000L);
        assertThat(limits.maxBytes()).isEqualTo(100L * 1024 * 1024);
        assertThat(limits.timeoutSeconds()).isEqualTo(30);
    }

    @Test
    void userWithoutDownloadPermissionIsForbiddenWithERR_PRJ_03() {
        DashboardDto dto = new DashboardDto(
                dashboardId, UUID.randomUUID(), "Dashboard", "", "{}", UUID.randomUUID(), Instant.now(), Instant.now()
        );
        when(dashboardRepository.findById(dashboardId)).thenReturn(Optional.of(dto));
        when(permissionService.can(eq(viewer), eq(Capability.DOWNLOAD), any(ResourceRef.class))).thenReturn(false);

        ExportRequest req = new ExportRequest(dashboardId, null, "CSV", "view", null, null, Map.of());

        assertThatThrownBy(() -> exportService.export(req, viewer))
                .isInstanceOf(DavsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_PRJ_03);
    }

    @Test
    void exportOver500kRowsThrowsERR_EXP_01() {
        DashboardDto dto = new DashboardDto(
                dashboardId, UUID.randomUUID(), "Dashboard", "", "{}", UUID.randomUUID(), Instant.now(), Instant.now()
        );
        when(dashboardRepository.findById(dashboardId)).thenReturn(Optional.of(dto));
        when(permissionService.can(eq(viewer), eq(Capability.DOWNLOAD), any(ResourceRef.class))).thenReturn(true);

        ExportRequest req = new ExportRequest(dashboardId, null, "CSV", "view", null, null, Map.of("__mock_exceed_limit__", true));

        assertThatThrownBy(() -> exportService.export(req, viewer))
                .isInstanceOf(DavsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_EXP_01);
    }

    @Test
    void exportCsvReturnsValidCsvPayloadWithUtf8Bom() {
        DashboardDto dto = new DashboardDto(
                dashboardId, UUID.randomUUID(), "Doanh số Q4", "", "{}", UUID.randomUUID(), Instant.now(), Instant.now()
        );
        when(dashboardRepository.findById(dashboardId)).thenReturn(Optional.of(dto));
        when(permissionService.can(eq(viewer), eq(Capability.DOWNLOAD), any(ResourceRef.class))).thenReturn(true);

        ExportRequest req = new ExportRequest(dashboardId, null, "CSV", "view", null, null, Map.of());

        ExportService.ExportResult res = exportService.export(req, viewer);

        assertThat(res).isNotNull();
        assertThat(res.filename()).contains("Doanh_số_Q4");
        assertThat(res.filename()).endsWith(".csv");
        // Verify UTF-8 BOM
        assertThat(res.data()[0]).isEqualTo((byte) 0xEF);
        assertThat(res.data()[1]).isEqualTo((byte) 0xBB);
        assertThat(res.data()[2]).isEqualTo((byte) 0xBF);
        String text = new String(res.data(), StandardCharsets.UTF_8);
        assertThat(text).contains("Tháng,Doanh số (triệu VND),Khu vực");
    }
}
