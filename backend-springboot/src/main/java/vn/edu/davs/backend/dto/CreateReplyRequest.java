package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateReplyRequest(
        @NotBlank(message = "Nội dung trả lời không được để trống")
        @Size(min = 1, max = 1000, message = "Nội dung trả lời từ 1 đến 1.000 ký tự")
        String text,

        @Size(max = 10, message = "Tối đa 10 người được nhắc tên (@)")
        List<String> mentions
) {
}
