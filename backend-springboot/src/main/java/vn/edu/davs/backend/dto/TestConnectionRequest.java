package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/datasources/test} (UC11).
 */
public record TestConnectionRequest(
        @NotBlank(message = "Loại nguồn dữ liệu không được để trống")
        String connectorType,
        String host,
        Integer port,
        String databaseName,
        String schemaName,
        String username,
        String password,
        Boolean driverMissing
) {
}
