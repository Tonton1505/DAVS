package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Interactive Action configuration on a dashboard (UC18).
 */
public record DashboardActionDto(
        String id,

        @NotBlank(message = "ERR-VIZ-05: Tên hành động không được để trống")
        @Size(min = 1, max = 100, message = "VAL-VIZ-17: Tên hành động gồm 1–100 ký tự")
        String name,

        @NotBlank(message = "VAL-VIZ-18: Loại hành động là bắt buộc")
        String type, // FILTER, HIGHLIGHT, URL, GO_TO_SHEET, PARAMETER

        UUID sourceSheetId,

        UUID targetSheetId,

        String trigger, // HOVER, SELECT, MENU

        String targetUrl,

        String parameterName,

        List<String> fields
) {
    public DashboardActionDto {
        fields = fields == null ? List.of() : List.copyOf(fields);
        trigger = trigger == null ? "SELECT" : trigger;
    }
}
