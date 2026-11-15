package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request payload for creating or updating a dashboard (UC17).
 */
public record SaveDashboardRequest(
        @NotBlank(message = "ERR-VIZ-05: Tên dashboard không được để trống")
        @Size(min = 1, max = 128, message = "VAL-VIZ-12: Tên dashboard gồm 1–128 ký tự")
        String name,

        @Size(max = 500, message = "VAL-VIZ-16: Mô tả tối đa 500 ký tự")
        String description,

        String sizeType,

        Integer width,

        Integer height,

        List<DashboardTileDto> tiles,

        List<DashboardActionDto> actions
) {
    public SaveDashboardRequest {
        sizeType = sizeType == null ? "FIXED" : sizeType;
        width = width == null ? 1366 : width;
        height = height == null ? 768 : height;
        tiles = tiles == null ? List.of() : List.copyOf(tiles);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
