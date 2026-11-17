package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Request payload for creating or updating a Row-Level Security (RLS) mapping (T-101).
 */
public record SaveRowFilterRequest(
        @NotNull(message = "Nguồn dữ liệu là bắt buộc")
        UUID datasourceId,

        @NotBlank(message = "Loại đối tượng là bắt buộc")
        @Pattern(regexp = "USER|GROUP", message = "Loại đối tượng phải là USER hoặc GROUP")
        String subjectType,

        @NotNull(message = "Mã người dùng hoặc nhóm là bắt buộc")
        UUID subjectId,

        @NotBlank(message = "Tên trường lọc là bắt buộc")
        @Size(max = 150, message = "Tên trường lọc tối đa 150 ký tự")
        String fieldName,

        @NotEmpty(message = "Danh sách giá trị cho phép không được để trống")
        List<String> allowedValues
) {
    public SaveRowFilterRequest {
        allowedValues = allowedValues == null ? List.of() : List.copyOf(allowedValues);
    }
}
