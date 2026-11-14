package vn.edu.davs.backend.dto;

import java.util.List;

/**
 * Response body for {@code POST /api/v1/datasources/test} (UC11).
 */
public record TestConnectionResponse(
        boolean success,
        String message,
        long estimatedRowCount,
        String suggestedMode,
        List<DiscoveredTableDto> tables
) {
    public TestConnectionResponse {
        tables = tables == null ? List.of() : List.copyOf(tables);
    }
}
