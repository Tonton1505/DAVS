package vn.edu.davs.backend.viz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import vn.edu.davs.backend.dao.VizRepository;
import vn.edu.davs.backend.dto.ParameterDto;
import vn.edu.davs.backend.dto.SaveParameterRequest;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.service.ParameterService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParameterServiceTests {

    private VizRepository vizRepository;
    private ParameterService parameterService;

    @BeforeEach
    void setUp() {
        vizRepository = mock(VizRepository.class);
        parameterService = new ParameterService(vizRepository);
    }

    @Test
    void testCreateParameterSuccess() {
        UUID wbId = UUID.randomUUID();
        SaveParameterRequest req = new SaveParameterRequest(
                "Top N",
                "INTEGER",
                "RANGE",
                List.of(),
                BigDecimal.ONE,
                BigDecimal.valueOf(50),
                BigDecimal.ONE,
                "10"
        );

        ParameterDto created = new ParameterDto(
                UUID.randomUUID(), wbId, "Top N", "INTEGER", "RANGE",
                List.of(), BigDecimal.ONE, BigDecimal.valueOf(50), BigDecimal.ONE, "10",
                Instant.now(), Instant.now()
        );
        when(vizRepository.existsParameterName(wbId, "Top N", null)).thenReturn(false);
        when(vizRepository.createParameter(wbId, req)).thenReturn(created);

        ParameterDto res = parameterService.createParameter(wbId, req);
        assertNotNull(res);
        assertEquals("Top N", res.name());
        assertEquals("10", res.currentValue());
    }

    @Test
    void testParameterOutOfRangeFails() {
        UUID wbId = UUID.randomUUID();
        // currentValue = 60 is out of range [1, 50] -> ERR-VIZ-02
        SaveParameterRequest req = new SaveParameterRequest(
                "Top N",
                "INTEGER",
                "RANGE",
                List.of(),
                BigDecimal.ONE,
                BigDecimal.valueOf(50),
                BigDecimal.ONE,
                "60"
        );
        when(vizRepository.existsParameterName(wbId, "Top N", null)).thenReturn(false);

        DavsException ex = assertThrows(DavsException.class, () -> parameterService.createParameter(wbId, req));
        assertEquals(ErrorCode.ERR_VIZ_02, ex.getErrorCode(), "Out of range value must throw ERR-VIZ-02");
    }

    @Test
    void testParameterNotInListFails() {
        UUID wbId = UUID.randomUUID();
        // currentValue = "North" is not in ["Hà Nội", "Đà Nẵng", "TP.HCM"] -> ERR-VIZ-02
        SaveParameterRequest req = new SaveParameterRequest(
                "Vùng chọn",
                "STRING",
                "LIST",
                List.of("Hà Nội", "Đà Nẵng", "TP.HCM"),
                null,
                null,
                null,
                "North"
        );
        when(vizRepository.existsParameterName(wbId, "Vùng chọn", null)).thenReturn(false);

        DavsException ex = assertThrows(DavsException.class, () -> parameterService.createParameter(wbId, req));
        assertEquals(ErrorCode.ERR_VIZ_02, ex.getErrorCode(), "Value not in allowed list must throw ERR-VIZ-02");
    }

    @Test
    void testCreateParameterDuplicateNameFails() {
        UUID wbId = UUID.randomUUID();
        SaveParameterRequest req = new SaveParameterRequest(
                "Top N",
                "INTEGER",
                "ALL",
                List.of(),
                null, null, null,
                "5"
        );
        when(vizRepository.existsParameterName(wbId, "Top N", null)).thenReturn(true);

        DavsException ex = assertThrows(DavsException.class, () -> parameterService.createParameter(wbId, req));
        assertEquals(ErrorCode.ERR_VIZ_05, ex.getErrorCode(), "Duplicate parameter name must throw ERR-VIZ-05");
    }
}
