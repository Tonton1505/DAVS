package com.davs.datasource.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDto implements Serializable {

    private String id;

    @NotBlank(message = "Tên bảng không được để trống")
    private String name;

    private String schema;

    private UUID sourceId;

    private String alias;
}
