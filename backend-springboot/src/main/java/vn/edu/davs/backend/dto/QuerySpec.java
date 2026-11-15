package vn.edu.davs.backend.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Specification for Query Engine execution (UC15).
 */
public record QuerySpec(
        UUID datasourceId,
        List<String> columns,
        List<String> rows,
        Map<String, String> aggregations,
        List<Map<String, Object>> filters,
        List<Map<String, String>> sorts,
        Integer limit
) {
    public QuerySpec {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : List.copyOf(rows);
        aggregations = aggregations == null ? Map.of() : Map.copyOf(aggregations);
        filters = filters == null ? List.of() : List.copyOf(filters);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
    }
}
