package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record QueryResultDto(
        List<Map<String, Object>> data,
        int totalRows,
        Instant dataAsOf,
        Map<String, Object> appliedFilters,
        boolean cached
) {
}
