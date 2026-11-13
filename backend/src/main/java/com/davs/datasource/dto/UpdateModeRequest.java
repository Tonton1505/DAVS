package com.davs.datasource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateModeRequest implements Serializable {

    @NotBlank(message = "Chế độ không được để trống")
    @Pattern(regexp = "^(LIVE|EXTRACT)$", message = "Chế độ phải là LIVE hoặc EXTRACT")
    private String mode;

    @Builder.Default
    private boolean confirmPerformanceImpact = false;
}
