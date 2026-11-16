package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request payload for formula syntax & reference validation (UC16).
 */
public record FormulaValidateRequest(
        UUID datasourceId,

        @NotBlank(message = "ERR-VIZ-01: Công thức không được để trống")
        @Size(max = 8000, message = "VAL-VIZ-07: Công thức tối đa 8.000 ký tự")
        String formula
) {
}
