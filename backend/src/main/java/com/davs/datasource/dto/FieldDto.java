package com.davs.datasource.dto;

import com.davs.datasource.domain.DataType;
import com.davs.datasource.domain.FieldRole;
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
public class FieldDto implements Serializable {

    private String id;

    @NotBlank(message = "Tên trường không được để trống")
    private String name;

    @Builder.Default
    private DataType dataType = DataType.STRING;

    private FieldRole role;

    @NotBlank(message = "Bảng chứa trường không được để trống")
    private String table;

    private UUID sourceId;

    private String alias;

    @Builder.Default
    private boolean hidden = false;
}
