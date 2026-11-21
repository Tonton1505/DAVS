package vn.edu.davs.backend.viz;


import vn.edu.davs.backend.service.impl.QueryServiceImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import vn.edu.davs.backend.dto.QueryResult;
import vn.edu.davs.backend.dto.QuerySpec;
import vn.edu.davs.backend.enu.ChartType;
import vn.edu.davs.backend.service.QueryService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryServiceTests {

    private QueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new QueryServiceImpl();
    }

    @Test
    void testInferChartTypeLineForTimeDimension() {
        QuerySpec spec = new QuerySpec(
                UUID.randomUUID(),
                List.of("Tháng (Ngày đặt hàng)"),
                List.of("SUM(Doanh số)"),
                Map.of(),
                List.of(),
                List.of(),
                12
        );

        ChartType chartType = queryService.inferChartType(spec);
        assertEquals(ChartType.LINE, chartType, "Time dimension + Measure must infer LINE (BUS-VIZ-01)");
    }

    @Test
    void testInferChartTypeBarForCategorical() {
        QuerySpec spec = new QuerySpec(
                UUID.randomUUID(),
                List.of("Vùng"),
                List.of("SUM(Doanh số)"),
                Map.of(),
                List.of(),
                List.of(),
                12
        );

        ChartType chartType = queryService.inferChartType(spec);
        assertEquals(ChartType.BAR, chartType, "Categorical dimension + Measure must infer BAR (BUS-VIZ-01)");
    }

    @Test
    void testInferChartTypeScatterForTwoMeasures() {
        QuerySpec spec = new QuerySpec(
                UUID.randomUUID(),
                List.of("SUM(Lợi nhuận)"),
                List.of("SUM(Doanh số)"),
                Map.of(),
                List.of(),
                List.of(),
                12
        );

        ChartType chartType = queryService.inferChartType(spec);
        assertEquals(ChartType.SCATTER, chartType, ">= 2 Measures must infer SCATTER (BUS-VIZ-01)");
    }

    @Test
    void testQueryOver100kMarksWarning() {
        QuerySpec spec = new QuerySpec(
                UUID.randomUUID(),
                List.of("Khách hàng"),
                List.of("Doanh số"),
                Map.of(),
                List.of(),
                List.of(),
                150_000
        );

        QueryResult result = queryService.executeQuery(spec);
        assertNotNull(result.warning(), "Must return warning when marks > 100,000 (BUS-VIZ-02, ERR-VIZ-04)");
        assertTrue(result.warning().contains("100.000"));
    }
}
