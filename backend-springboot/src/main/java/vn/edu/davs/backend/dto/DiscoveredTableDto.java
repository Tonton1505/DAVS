package vn.edu.davs.backend.dto;

import java.util.List;
import java.util.Map;

/**
 * Discovered table metadata and preview rows for a data source (UC11).
 */
public record DiscoveredTableDto(
        String tableName,
        String schemaName,
        long rowCount,
        String suggestedMode,
        List<ColumnSchemaDto> columns,
        List<Map<String, Object>> previewRows
) {
    public DiscoveredTableDto {
        columns = columns == null ? List.of() : List.copyOf(columns);
        previewRows = previewRows == null ? List.of() : List.copyOf(previewRows);
    }

    public record ColumnSchemaDto(
            String name,
            String dataType,
            String defaultRole
    ) {
    }
}
