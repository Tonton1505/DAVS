package vn.edu.davs.backend.viz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import vn.edu.davs.backend.dao.VizRepository;
import vn.edu.davs.backend.dto.DashboardDto;
import vn.edu.davs.backend.dto.DashboardTileDto;
import vn.edu.davs.backend.dto.SaveDashboardRequest;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.service.DashboardService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardServiceTests {

    private VizRepository vizRepository;
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        vizRepository = mock(VizRepository.class);
        dashboardService = new DashboardService(vizRepository);
    }

    @Test
    void testCreateDashboardSuccess() {
        UUID wbId = UUID.randomUUID();
        DashboardTileDto tile = new DashboardTileDto("tile-1", UUID.randomUUID(), "Doanh số", "SHEET", 0, 0, 6, 4, null);
        SaveDashboardRequest req = new SaveDashboardRequest(
                "Tổng quan kinh doanh",
                "Mô tả dashboard Q4",
                "FIXED",
                1366,
                768,
                List.of(tile),
                List.of()
        );

        DashboardDto created = new DashboardDto(
                UUID.randomUUID(), wbId, "Tổng quan kinh doanh", "Mô tả dashboard Q4",
                "FIXED", 1366, 768, List.of(tile), List.of(), Instant.now(), Instant.now()
        );
        when(vizRepository.existsDashboardName(wbId, "Tổng quan kinh doanh", null)).thenReturn(false);
        when(vizRepository.createDashboard(wbId, req)).thenReturn(created);

        DashboardDto res = dashboardService.createDashboard(wbId, req);
        assertNotNull(res);
        assertEquals("Tổng quan kinh doanh", res.name());
        verify(vizRepository).createDashboard(wbId, req);
    }

    @Test
    void testCreateDashboardDuplicateNameFails() {
        UUID wbId = UUID.randomUUID();
        SaveDashboardRequest req = new SaveDashboardRequest(
                "Tổng quan kinh doanh",
                null,
                "FIXED",
                1366,
                768,
                List.of(),
                List.of()
        );
        when(vizRepository.existsDashboardName(wbId, "Tổng quan kinh doanh", null)).thenReturn(true);

        DavsException ex = assertThrows(DavsException.class, () -> dashboardService.createDashboard(wbId, req));
        assertEquals(ErrorCode.ERR_VIZ_05, ex.getErrorCode(), "Duplicate dashboard name must throw ERR-VIZ-05");
    }

    @Test
    void testUpdateDashboardSuccess() {
        UUID dashId = UUID.randomUUID();
        UUID wbId = UUID.randomUUID();
        DashboardDto existing = new DashboardDto(dashId, wbId, "Dash cũ", null, "FIXED",
                1366, 768, List.of(), List.of(), Instant.now(), Instant.now());
        when(vizRepository.findDashboardById(dashId)).thenReturn(Optional.of(existing));
        when(vizRepository.existsDashboardName(wbId, "Dash mới", dashId)).thenReturn(false);

        SaveDashboardRequest req = new SaveDashboardRequest("Dash mới", "Mô tả", "FIXED", 1366, 768, List.of(), List.of());
        dashboardService.updateDashboard(dashId, req);
        verify(vizRepository).updateDashboard(dashId, req);
    }
}
