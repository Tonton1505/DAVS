package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Request payload for creating or editing a parameter (UC16).
 */
public record SaveParameterRequest(
        @NotBlank(message = "ERR-VIZ-05: Tên tham số không được để trống")
        @Size(min = 1, max = 128, message = "VAL-VIZ-08: Tên tham số gồm 1–128 ký tự")
        String name,

        @NotBlank(message = "VAL-VIZ-09: Kiểu dữ liệu tham số là bắt buộc")
        String dataType,

        @NotBlank(message = "VAL-VIZ-10: Giá trị cho phép là bắt buộc")
        String allowableValuesType,

        List<String> listValues,

        BigDecimal rangeMin,

        BigDecimal rangeMax,

        BigDecimal rangeStep,

        @NotBlank(message = "VAL-VIZ-11: Giá trị hiện tại là bắt buộc")
        String currentValue
) {
    public SaveParameterRequest {
        listValues = listValues == null ? List.of() : List.copyOf(listValues);
    }
}
