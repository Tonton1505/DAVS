package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/label-categories} (UC09).
 */
public record CreateLabelCategoryRequest(
        @NotBlank(message = "Tên danh mục nhãn không được để trống")
        @Size(max = 100, message = "Tên danh mục nhãn tối đa 100 ký tự")
        String name,
        @Size(max = 250, message = "Mô tả tối đa 250 ký tự")
        String description,
        Boolean isWarning
) {
}
