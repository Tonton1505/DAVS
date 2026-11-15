package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code PUT /api/v1/datasources/{id}/mode} (UC14).
 */
public record UpdateDatasourceModeRequest(
        @NotBlank(message = "Chế độ kết nối không được để trống")
        String mode
) {
}
