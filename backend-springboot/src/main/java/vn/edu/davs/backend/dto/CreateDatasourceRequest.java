package vn.edu.davs.backend.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/datasources} (UC11).
 */
public record CreateDatasourceRequest(
        String name,
        String description,
        String connectorType,
        String mode,
        UUID projectId,
        String host,
        Integer port,
        String databaseName,
        String schemaName,
        String username,
        String password,
        Long estimatedRowCount,
        String csvContent,
        List<DiscoveredTableDto> tables
) {
}
