package com.davs.datasource.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataModelDto implements Serializable {

    private UUID id;

    private UUID datasourceId;

    @NotNull(message = "Danh sách bảng không được để trống")
    @Valid
    @Builder.Default
    private List<TableDto> tables = new ArrayList<>();

    @NotNull(message = "Danh sách quan hệ không được để trống")
    @Valid
    @Builder.Default
    private List<RelationshipDto> relationships = new ArrayList<>();

    @NotNull(message = "Danh sách trường không được để trống")
    @Valid
    @Builder.Default
    private List<FieldDto> fields = new ArrayList<>();

    @Builder.Default
    private Integer version = 1;

    private Instant createdAt;

    private Instant updatedAt;
}
