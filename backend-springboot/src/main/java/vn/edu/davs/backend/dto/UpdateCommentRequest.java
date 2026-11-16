package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @NotBlank(message = "Nội dung bình luận không được để trống")
        @Size(min = 1, max = 1000, message = "Nội dung bình luận từ 1 đến 1.000 ký tự")
        String text
) {
}
