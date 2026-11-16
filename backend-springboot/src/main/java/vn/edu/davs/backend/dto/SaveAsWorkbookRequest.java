package vn.edu.davs.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SaveAsWorkbookRequest(
        @NotNull(message = "sourceWorkbookId không được để trống")
        UUID sourceWorkbookId,

        @NotNull(message = "targetProjectId không được để trống")
        UUID targetProjectId,

        @NotBlank(message = "Tên workbook mới không được để trống")
        @Size(min = 1, max = 255, message = "Tên workbook từ 1 đến 255 ký tự")
        String newName,

        String description
) {
}
