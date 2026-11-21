package vn.edu.davs.backend.viewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.dao.DashboardDao;
import vn.edu.davs.backend.dao.WorkbookDao;
import vn.edu.davs.backend.dto.DashboardDto;
import vn.edu.davs.backend.dto.DashboardViewDto;
import vn.edu.davs.backend.dto.QueryRequest;
import vn.edu.davs.backend.dto.QueryResultDto;
import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.model.ResourceRef;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.service.DashboardService;
import vn.edu.davs.backend.service.PermissionService;
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
class DashboardViewTests {

    @Mock
    private DashboardDao dashboardRepository;
    @Mock
    private WorkbookDao workbookRepository;
    @Mock
    private PermissionService permissionService;
    @Mock
    private AuditService auditService;

    private DashboardService dashboardService;

    private final DavsUser viewer = DavsUser.of(UUID.randomUUID(), "viewer@davs.vn", "Viewer User", SiteRole.VIEWER);
    private final UUID dashboardId = UUID.randomUUID();
    private final UUID workbookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                dashboardRepository,
                workbookRepository,
                permissionService,
                auditService,
                new ObjectMapper()
        );
    }

    @Test
    void viewerCanViewDashboardAndReceiveEffectivePermissions() {
        DashboardDto dto = new DashboardDto(
                dashboardId, workbookId, "Tổng quan kinh doanh", "Mô tả", "{}", viewer.getId(), Instant.now(), Instant.now()
        );
        when(dashboardRepository.findById(dashboardId)).thenReturn(Optional.of(dto));
        when(permissionService.can(eq(viewer), eq(Capability.VIEW), any(ResourceRef.class))).thenReturn(true);
        when(permissionService.can(eq(viewer), eq(Capability.INTERACT), any(ResourceRef.class))).thenReturn(true);
        when(permissionService.can(eq(viewer), eq(Capability.FILTER), any(ResourceRef.class))).thenReturn(true);
        when(permissionService.can(eq(viewer), eq(Capability.DOWNLOAD), any(ResourceRef.class))).thenReturn(false);

        DashboardViewDto view = dashboardService.getDashboardView(dashboardId, viewer);

        assertThat(view).isNotNull();
        assertThat(view.name()).isEqualTo("Tổng quan kinh doanh");
        assertThat(view.canView()).isTrue();
        assertThat(view.canInteract()).isTrue();
        assertThat(view.canFilter()).isTrue();
        assertThat(view.canDownload()).isFalse();
        assertThat(view.dataAsOf()).isNotNull();
    }

    @Test
    void userWithoutViewPermissionIsForbiddenWithERR_PRJ_03() {
        DashboardDto dto = new DashboardDto(
                dashboardId, workbookId, "Báo cáo nội bộ", "Mô tả", "{}", UUID.randomUUID(), Instant.now(), Instant.now()
        );
        when(dashboardRepository.findById(dashboardId)).thenReturn(Optional.of(dto));
        when(permissionService.can(eq(viewer), eq(Capability.VIEW), any(ResourceRef.class))).thenReturn(false);

        assertThatThrownBy(() -> dashboardService.getDashboardView(dashboardId, viewer))
                .isInstanceOf(DavsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_PRJ_03);
    }

    @Test
    void executeQueryAppliesFiltersAndReturnsCorrectData() {
        when(permissionService.can(eq(viewer), eq(Capability.VIEW), any(ResourceRef.class))).thenReturn(true);

        QueryRequest req = new QueryRequest(
                dashboardId, null, Map.of("region", "Miền Bắc"), Map.of("topProducts", 5), null
        );

        QueryResultDto result = dashboardService.executeQuery(req, viewer);

        assertThat(result).isNotNull();
        assertThat(result.totalRows()).isEqualTo(12);
        assertThat(result.appliedFilters()).containsEntry("region", "Miền Bắc");
        assertThat(result.dataAsOf()).isNotNull();
    }
}
