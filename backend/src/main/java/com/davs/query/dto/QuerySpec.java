package com.davs.query.dto;

import com.davs.datasource.dto.FieldDto;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuerySpec implements Serializable {

    @NotNull(message = "Nguồn dữ liệu không được để trống")
    private UUID datasourceId;

    @Builder.Default
    private List<FieldDto> columns = new ArrayList<>();

    @Builder.Default
    private List<FieldDto> rows = new ArrayList<>();

    @Builder.Default
    private List<Map<String, Object>> filters = new ArrayList<>();

    @Builder.Default
    private int limit = 1000;
}
