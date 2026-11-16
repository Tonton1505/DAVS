package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.ChartType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Request payload for creating or updating a sheet (UC15).
 */
public record SaveSheetRequest(
        @NotBlank(message = "ERR-VIZ-05: Tên sheet không được để trống")
        @Size(min = 1, max = 128, message = "VAL-VIZ-05: Tên sheet gồm 1–128 ký tự")
        String name,

        ChartType chartType,

        Map<String, Object> definition,

        TooltipConfigDto tooltipConfig
) {
    public SaveSheetRequest {
        chartType = chartType == null ? ChartType.BAR : chartType;
        definition = definition == null ? Map.of() : Map.copyOf(definition);
    }
}
