package com.davs.datasource.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestRelationshipsRequest implements Serializable {

    @NotEmpty(message = "Danh sách bảng không được để trống")
    @Valid
    private List<TableDto> tables;

    @NotEmpty(message = "Danh sách trường không được để trống")
    @Valid
    private List<FieldDto> fields;
}
