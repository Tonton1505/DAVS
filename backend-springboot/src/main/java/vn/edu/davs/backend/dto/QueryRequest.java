package vn.edu.davs.backend.dto;

import java.util.Map;
import java.util.UUID;

public record QueryRequest(
        UUID dashboardId,
        UUID sheetId,
        Map<String, Object> filters,
        Map<String, Object> parameters,
        String selectedMark
) {
}
