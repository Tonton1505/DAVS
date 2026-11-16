package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

public record CreateCustomViewRequest(
        @NotNull(message = "dashboardId không được để trống")
        UUID dashboardId,

        @NotBlank(message = "Tên view tùy chỉnh không được để trống")
        @Size(min = 1, max = 255, message = "Tên view từ 1 đến 255 ký tự")
        String name,

        Map<String, Object> filters,
        Map<String, Object> parameters,
        Boolean isDefault,
        Boolean isShared
) {
}
