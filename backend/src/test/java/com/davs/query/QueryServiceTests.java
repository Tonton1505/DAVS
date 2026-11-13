package com.davs.query;

import com.davs.datasource.domain.DataType;
import com.davs.datasource.domain.FieldRole;
import com.davs.datasource.dto.FieldDto;
import com.davs.query.dto.QueryResult;
import com.davs.query.dto.QuerySpec;
import com.davs.query.service.QueryService;
import com.davs.query.service.impl.QueryServiceImpl;
import com.davs.viz.domain.ChartType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryServiceTests {

    private QueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new QueryServiceImpl();
    }

    @Test
    @DisplayName("BUS-VIZ-01: 1 Dimension thời gian + 1 Measure -> suy luận biểu đồ Đường (LINE)")
    void inferChartType_shouldInferLineForTimeAndMeasure() {
        FieldDto timeCol = FieldDto.builder().name("NgayDatHang").dataType(DataType.DATE).role(FieldRole.DIMENSION).build();
        FieldDto salesRow = FieldDto.builder().name("DoanhSo").dataType(DataType.NUMBER).role(FieldRole.MEASURE).build();

        ChartType type = queryService.inferChartType(List.of(timeCol), List.of(salesRow));
        assertThat(type).isEqualTo(ChartType.LINE);
    }

    @Test
    @DisplayName("BUS-VIZ-01: 1 Dimension phân loại + 1 Measure -> suy luận biểu đồ Cột (BAR)")
    void inferChartType_shouldInferBarForCategoryAndMeasure() {
        FieldDto regionCol = FieldDto.builder().name("Vung").dataType(DataType.STRING).role(FieldRole.DIMENSION).build();
        FieldDto salesRow = FieldDto.builder().name("DoanhSo").dataType(DataType.NUMBER).role(FieldRole.MEASURE).build();

        ChartType type = queryService.inferChartType(List.of(regionCol), List.of(salesRow));
        assertThat(type).isEqualTo(ChartType.BAR);
    }

    @Test
    @DisplayName("BUS-VIZ-01: 2 Measures không có dimension -> suy luận biểu đồ Phân tán (SCATTER)")
    void inferChartType_shouldInferScatterForTwoMeasures() {
        FieldDto salesCol = FieldDto.builder().name("DoanhSo").dataType(DataType.NUMBER).role(FieldRole.MEASURE).build();
        FieldDto profitRow = FieldDto.builder().name("LoiNhuan").dataType(DataType.NUMBER).role(FieldRole.MEASURE).build();

        ChartType type = queryService.inferChartType(List.of(salesCol), List.of(profitRow));
        assertThat(type).isEqualTo(ChartType.SCATTER);
    }

    @Test
    @DisplayName("UC15 Luồng chính: Thực thi QuerySpec trả về đúng cấu trúc QueryResult")
    void executeQuery_shouldReturnDataRowsAndMarks() {
        QuerySpec spec = QuerySpec.builder()
                .datasourceId(UUID.randomUUID())
                .columns(List.of(FieldDto.builder().name("Thang").dataType(DataType.STRING).role(FieldRole.DIMENSION).build()))
                .rows(List.of(FieldDto.builder().name("DoanhSo").dataType(DataType.NUMBER).role(FieldRole.MEASURE).build()))
                .limit(12)
                .build();

        QueryResult res = queryService.executeQuery(spec);

        assertThat(res).isNotNull();
        assertThat(res.getColumns()).containsExactly("Thang", "DoanhSo");
        assertThat(res.getRows()).hasSize(12);
        assertThat(res.getTotalMarks()).isEqualTo(12L);
        assertThat(res.getRecommendedChartType()).isEqualTo(ChartType.BAR);
        assertThat(res.getWarning()).isNull();
    }
}
