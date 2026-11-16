package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record ExportRequest(
        @NotNull(message = "dashboardId không được để trống")
        UUID dashboardId,

        UUID sheetId,

        @NotBlank(message = "Định dạng xuất không được để trống")
        String format,

        String scope,

        String orientation,

        String pageSize,

        Map<String, Object> filters
) {
}
