package vn.edu.davs.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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

        @JsonAlias({"paperOrientation"})
        String orientation,

        @JsonAlias({"paperSize"})
        String pageSize,

        @JsonAlias({"customParameters"})
        Map<String, Object> filters
) {
}
