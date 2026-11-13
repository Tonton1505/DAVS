package com.davs.datasource.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewModelResponse implements Serializable {

    @Builder.Default
    private List<String> columns = new ArrayList<>();

    @Builder.Default
    private List<Map<String, Object>> rows = new ArrayList<>();

    @Builder.Default
    private int totalRows = 0;

    @Builder.Default
    private int affectedRows = 0;

    @Builder.Default
    private Map<String, Long> nullCountByField = new HashMap<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
