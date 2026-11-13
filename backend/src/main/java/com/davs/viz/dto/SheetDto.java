package com.davs.viz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SheetDto implements Serializable {

    private UUID id;

    private UUID workbookId;

    @NotBlank(message = "Tên sheet không được để trống")
    @Size(min = 1, max = 128, message = "Tên sheet phải từ 1 đến 128 ký tự")
    private String name;

    @Builder.Default
    private SheetDefinitionDto definition = new SheetDefinitionDto();

    private Instant createdAt;

    private Instant updatedAt;
}
