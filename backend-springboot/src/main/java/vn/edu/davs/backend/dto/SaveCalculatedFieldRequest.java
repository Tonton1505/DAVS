package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating or editing a calculated field (UC16).
 */
public record SaveCalculatedFieldRequest(
        @NotBlank(message = "ERR-VIZ-05: Tên trường tính toán không được để trống")
        @Size(min = 1, max = 128, message = "VAL-VIZ-06: Tên trường gồm 1–128 ký tự")
        String name,

        @NotBlank(message = "ERR-VIZ-01: Công thức không được để trống")
        @Size(max = 8000, message = "VAL-VIZ-07: Công thức tối đa 8.000 ký tự")
        String formula,

        String returnType,

        String role
) {
    public SaveCalculatedFieldRequest {
        returnType = returnType == null ? "STRING" : returnType;
        role = role == null ? "DIMENSION" : role;
    }
}
