package vn.edu.davs.backend.dto;

import java.util.List;
import java.util.Map;

/**
 * Response body for {@code POST /api/v1/datasources/{id}/model/preview} (UC13).
 */
public record ModelPreviewResponse(
        List<DataModelDto.ModelFieldDto> fields,
        List<Map<String, Object>> rows,
        int rowCount
) {
    public ModelPreviewResponse {
        fields = fields == null ? List.of() : List.copyOf(fields);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
