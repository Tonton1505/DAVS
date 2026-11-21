package vn.edu.davs.backend.viz;


import vn.edu.davs.backend.service.impl.FormulaServiceImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import vn.edu.davs.backend.dao.VizRepository;
import vn.edu.davs.backend.dto.FormulaValidateResponse;
import vn.edu.davs.backend.dto.SaveCalculatedFieldRequest;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.service.FormulaService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FormulaServiceTests {

    private VizRepository vizRepository;
    private FormulaService formulaService;

    @BeforeEach
    void setUp() {
        vizRepository = mock(VizRepository.class);
        formulaService = new FormulaServiceImpl(vizRepository);
    }

    @Test
    void testValidFormulaCalculation() {
        UUID dsId = UUID.randomUUID();
        FormulaValidateResponse response = formulaService.validateFormula(
                dsId,
                "SUM([Lợi nhuận]) / SUM([Doanh số])",
                "Biên lợi nhuận"
        );

        assertNotNull(response);
        assertTrue(response.valid());
        assertEquals("NUMBER", response.returnType());
        assertEquals("MEASURE", response.role());
    }

    @Test
    void testFormulaUnbalancedParensFails() {
        UUID dsId = UUID.randomUUID();
        DavsException ex = assertThrows(DavsException.class, () ->
                formulaService.validateFormula(dsId, "SUM([Doanh số] + 10", "Test")
        );
        assertEquals(ErrorCode.ERR_VIZ_01, ex.getErrorCode(), "Unbalanced parens must throw ERR-VIZ-01");
    }

    @Test
    void testFormulaCircularReferenceFails() {
        UUID dsId = UUID.randomUUID();
        // Formula references target field name -> circular reference (BUS-VIZ-06, ERR-VIZ-06)
        DavsException ex = assertThrows(DavsException.class, () ->
                formulaService.validateFormula(dsId, "[Doanh thu thuần] * 1.1", "Doanh thu thuần")
        );
        assertEquals(ErrorCode.ERR_VIZ_06, ex.getErrorCode(), "Circular reference must throw ERR-VIZ-06");
    }

    @Test
    void testFormulaUnknownFieldFails() {
        UUID dsId = UUID.randomUUID();
        DavsException ex = assertThrows(DavsException.class, () ->
                formulaService.validateFormula(dsId, "[UNKNOWN_FIELD] + 5", "Truong_Moi")
        );
        assertEquals(ErrorCode.ERR_VIZ_03, ex.getErrorCode(), "Unknown field must throw ERR-VIZ-03");
    }

    @Test
    void testCreateCalculatedFieldDuplicateNameFails() {
        UUID dsId = UUID.randomUUID();
        SaveCalculatedFieldRequest req = new SaveCalculatedFieldRequest(
                "Lợi nhuận gộp",
                "[Doanh số] - [Chi phí]",
                "NUMBER",
                "MEASURE"
        );
        when(vizRepository.existsCalculatedFieldName(dsId, "Lợi nhuận gộp", null)).thenReturn(true);

        DavsException ex = assertThrows(DavsException.class, () ->
                formulaService.createCalculatedField(dsId, req)
        );
        assertEquals(ErrorCode.ERR_VIZ_05, ex.getErrorCode(), "Duplicate calculated field name must throw ERR-VIZ-05 (BUS-VIZ-05)");
    }
}
