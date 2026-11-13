package com.davs.datasource.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewModelRequest implements Serializable {

    @NotNull(message = "Mô hình dữ liệu không được để trống")
    @Valid
    private DataModelDto model;

    @Builder.Default
    private int limit = 100;
}
