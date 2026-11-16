package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateDashboardRequest(
        @NotNull(message = "workbookId không được để trống")
        UUID workbookId,

        @NotBlank(message = "Tên dashboard không được để trống")
        @Size(max = 255, message = "Tên dashboard tối đa 255 ký tự")
        String name,

        @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
        String description,

        String definition
) {
}
