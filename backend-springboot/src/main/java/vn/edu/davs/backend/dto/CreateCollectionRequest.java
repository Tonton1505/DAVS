package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/collections} (UC06 [6d]).
 */
public record CreateCollectionRequest(
        @NotBlank(message = "Tên bộ sưu tập không được để trống")
        @Size(max = 128, message = "Tên bộ sưu tập tối đa 128 ký tự")
        String name,
        @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
        String description
) {
}
