package vn.edu.davs.backend.viz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import vn.edu.davs.backend.dao.VizRepository;
import vn.edu.davs.backend.dto.SaveSheetRequest;
import vn.edu.davs.backend.dto.SheetDto;
import vn.edu.davs.backend.dto.TooltipConfigDto;
import vn.edu.davs.backend.enu.ChartType;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.service.SheetService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SheetServiceTests {

    private VizRepository vizRepository;
    private SheetService sheetService;

    @BeforeEach
    void setUp() {
        vizRepository = mock(VizRepository.class);
        sheetService = new SheetService(vizRepository);
    }

    @Test
    void testCreateSheetSuccess() {
        UUID workbookId = UUID.randomUUID();
        SaveSheetRequest req = new SaveSheetRequest("Doanh số theo tháng", ChartType.LINE, Map.of(), null);

        SheetDto created = new SheetDto(UUID.randomUUID(), workbookId, "Doanh số theo tháng",
                ChartType.LINE, Map.of(), null, Instant.now(), Instant.now());
        when(vizRepository.existsSheetName(workbookId, "Doanh số theo tháng", null)).thenReturn(false);
        when(vizRepository.createSheet(workbookId, req)).thenReturn(created);

        SheetDto result = sheetService.createSheet(workbookId, req);
        assertNotNull(result);
        assertEquals("Doanh số theo tháng", result.name());
        verify(vizRepository).createSheet(workbookId, req);
    }

    @Test
    void testCreateSheetDuplicateNameFails() {
        UUID workbookId = UUID.randomUUID();
        SaveSheetRequest req = new SaveSheetRequest("Doanh số theo tháng", ChartType.BAR, Map.of(), null);
        when(vizRepository.existsSheetName(workbookId, "Doanh số theo tháng", null)).thenReturn(true);

        DavsException ex = assertThrows(DavsException.class, () -> sheetService.createSheet(workbookId, req));
        assertEquals(ErrorCode.ERR_VIZ_05, ex.getErrorCode(), "Must throw ERR-VIZ-05 for duplicate sheet name (BUS-VIZ-04)");
    }

    @Test
    void testUpdateSheetDuplicateNameFails() {
        UUID sheetId = UUID.randomUUID();
        UUID workbookId = UUID.randomUUID();
        SheetDto existing = new SheetDto(sheetId, workbookId, "Sheet cũ", ChartType.BAR, Map.of(), null, Instant.now(), Instant.now());
        when(vizRepository.findSheetById(sheetId)).thenReturn(Optional.of(existing));
        when(vizRepository.existsSheetName(workbookId, "Sheet trùng", sheetId)).thenReturn(true);

        SaveSheetRequest req = new SaveSheetRequest("Sheet trùng", ChartType.BAR, Map.of(), null);
        DavsException ex = assertThrows(DavsException.class, () -> sheetService.updateSheet(sheetId, req));
        assertEquals(ErrorCode.ERR_VIZ_05, ex.getErrorCode());
    }

    @Test
    void testUpdateSheetTooltipSuccess() {
        UUID sheetId = UUID.randomUUID();
        UUID workbookId = UUID.randomUUID();
        SheetDto existing = new SheetDto(sheetId, workbookId, "Sheet 1", ChartType.BAR, Map.of(), null, Instant.now(), Instant.now());
        when(vizRepository.findSheetById(sheetId)).thenReturn(Optional.of(existing));

        TooltipConfigDto tooltip = new TooltipConfigDto("Doanh số: <Doanh số>", List.of("Doanh số"));
        sheetService.updateTooltip(sheetId, tooltip);
        verify(vizRepository).updateSheetTooltip(sheetId, tooltip);
    }
}
