package com.davs.viz.dto;

import com.davs.datasource.dto.FieldDto;
import com.davs.viz.domain.ChartType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SheetDefinitionDto implements Serializable {

    @Builder.Default
    private ChartType chartType = ChartType.BAR;

    @Builder.Default
    private List<FieldDto> columns = new ArrayList<>();

    @Builder.Default
    private List<FieldDto> rows = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> marks = new HashMap<>();

    @Builder.Default
    private List<Map<String, Object>> filters = new ArrayList<>();
}
