package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Semantic data model DTO returned by {@code /api/v1/datasources/{id}/model} (UC13).
 */
public record DataModelDto(
        UUID datasourceId,
        String datasourceName,
        List<ModelTableDto> tables,
        List<ModelRelationshipDto> relationships,
        List<ModelFieldDto> fields,
        List<ModelRelationshipDto> suggestedRelationships,
        Instant updatedAt
) {
    public DataModelDto {
        tables = tables == null ? List.of() : List.copyOf(tables);
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
        fields = fields == null ? List.of() : List.copyOf(fields);
        suggestedRelationships = suggestedRelationships == null ? List.of() : List.copyOf(suggestedRelationships);
    }

    public record ModelTableDto(
            String id,
            String tableName,
            String schemaName,
            UUID datasourceId,
            List<String> columns
    ) {
        public ModelTableDto {
            columns = columns == null ? List.of() : List.copyOf(columns);
        }
    }

    public record ModelRelationshipDto(
            String id,
            String leftTable,
            String leftKey,
            String leftKeyType,
            UUID leftDatasourceId,
            String rightTable,
            String rightKey,
            String rightKeyType,
            UUID rightDatasourceId,
            String joinType,
            String operator
    ) {
    }

    public record ModelFieldDto(
            String name,
            String alias,
            String sourceTable,
            String dataType,
            String role,
            String defaultAggregation,
            boolean hidden,
            String description,
            String folder,
            String label
    ) {
    }
}
