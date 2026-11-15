package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.ChartType;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Result returned by Query Engine execution (UC15).
 */
public record QueryResult(
        List<String> columns,
        List<Map<String, Object>> rows,
        boolean truncated,
        Instant dataAsOf,
        int marksCount,
        ChartType inferredChartType,
        String warning
) {
    public QueryResult {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
