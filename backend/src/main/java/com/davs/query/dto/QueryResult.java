package com.davs.query.dto;

import com.davs.viz.domain.ChartType;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
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
public class QueryResult implements Serializable {

    @Builder.Default
    private List<String> columns = new ArrayList<>();

    @Builder.Default
    private List<Map<String, Object>> rows = new ArrayList<>();

    @Builder.Default
    private long totalMarks = 0L;

    @Builder.Default
    private boolean truncated = false;

    @Builder.Default
    private Instant dataAsOf = Instant.now();

    @Builder.Default
    private ChartType recommendedChartType = ChartType.BAR;

    private String warning;
}
