package vn.edu.davs.backend.viz;


import vn.edu.davs.backend.service.impl.DashboardActionServiceImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import vn.edu.davs.backend.dao.VizRepository;
import vn.edu.davs.backend.dto.DashboardActionDto;
import vn.edu.davs.backend.dto.DashboardDto;
import vn.edu.davs.backend.dto.SaveActionsRequest;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.service.DashboardActionService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardActionServiceTests {

    private VizRepository vizRepository;
    private DashboardActionService actionService;

    @BeforeEach
    void setUp() {
        vizRepository = mock(VizRepository.class);
        actionService = new DashboardActionServiceImpl(vizRepository);
    }

    @Test
    void testSaveActionsSuccess() {
        UUID dashId = UUID.randomUUID();
        UUID wbId = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();

        DashboardDto existing = new DashboardDto(dashId, wbId, "Dash", null, "FIXED",
                1366, 768, List.of(), List.of(), Instant.now(), Instant.now());
        when(vizRepository.findDashboardById(dashId)).thenReturn(Optional.of(existing));

        DashboardActionDto action = new DashboardActionDto(
                "act-1", "Lọc theo vùng", "FILTER", s1, s2, "SELECT", null, null, List.of("Vùng")
        );
        SaveActionsRequest req = new SaveActionsRequest(List.of(action));

        actionService.saveActions(wbId, dashId, req);
        verify(vizRepository).updateDashboardActions(dashId, List.of(action));
    }

    @Test
    void testActionSourceSameAsTargetFails() {
        UUID dashId = UUID.randomUUID();
        UUID wbId = UUID.randomUUID();
        UUID sheetId = UUID.randomUUID();

        DashboardDto existing = new DashboardDto(dashId, wbId, "Dash", null, "FIXED",
                1366, 768, List.of(), List.of(), Instant.now(), Instant.now());
        when(vizRepository.findDashboardById(dashId)).thenReturn(Optional.of(existing));

        // Source == Target -> [5a] ERR-VIZ-07
        DashboardActionDto invalid = new DashboardActionDto(
                "act-1", "Trùng sheet", "FILTER", sheetId, sheetId, "SELECT", null, null, List.of()
        );
        SaveActionsRequest req = new SaveActionsRequest(List.of(invalid));

        DavsException ex = assertThrows(DavsException.class, () -> actionService.saveActions(wbId, dashId, req));
        assertEquals(ErrorCode.ERR_VIZ_07, ex.getErrorCode(), "Source sheet equals target sheet must throw ERR-VIZ-07");
    }

    @Test
    void testActionUrlInsecureOrUntrustedFails() {
        UUID dashId = UUID.randomUUID();
        UUID wbId = UUID.randomUUID();

        DashboardDto existing = new DashboardDto(dashId, wbId, "Dash", null, "FIXED",
                1366, 768, List.of(), List.of(), Instant.now(), Instant.now());
        when(vizRepository.findDashboardById(dashId)).thenReturn(Optional.of(existing));

        // Insecure http URL -> BUS-VIZ-12 must reject
        DashboardActionDto httpAction = new DashboardActionDto(
                "act-url", "Mở liên kết", "URL", null, null, "SELECT", "http://bao-cao.congty.vn/view", null, List.of()
        );
        SaveActionsRequest req = new SaveActionsRequest(List.of(httpAction));

        assertThrows(DavsException.class, () -> actionService.saveActions(wbId, dashId, req));
    }

    @Test
    void testActionMax20LimitEnforced() {
        UUID dashId = UUID.randomUUID();
        UUID wbId = UUID.randomUUID();

        DashboardDto existing = new DashboardDto(dashId, wbId, "Dash", null, "FIXED",
                1366, 768, List.of(), List.of(), Instant.now(), Instant.now());
        when(vizRepository.findDashboardById(dashId)).thenReturn(Optional.of(existing));

        // 21 actions -> exceeds BUS-VIZ-13 limit of 20
        List<DashboardActionDto> list = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            list.add(new DashboardActionDto("act-" + i, "Action " + i, "FILTER",
                    UUID.randomUUID(), UUID.randomUUID(), "SELECT", null, null, List.of()));
        }
        SaveActionsRequest req = new SaveActionsRequest(list);

        DavsException ex = assertThrows(DavsException.class, () -> actionService.saveActions(wbId, dashId, req));
        assertNotNull(ex);
    }

    @Test
    void testGetTrustedDomains() {
        List<String> domains = actionService.getTrustedDomains();
        assertNotNull(domains);
        assertTrue(domains.contains("bao-cao.congty.vn"));
    }
}
