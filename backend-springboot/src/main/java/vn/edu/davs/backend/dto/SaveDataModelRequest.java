package vn.edu.davs.backend.dto;

import java.util.List;

/**
 * Request body for {@code PUT /api/v1/datasources/{id}/model} and preview (UC13).
 */
public record SaveDataModelRequest(
        List<DataModelDto.ModelTableDto> tables,
        List<DataModelDto.ModelRelationshipDto> relationships,
        List<DataModelDto.ModelFieldDto> fields
) {
}
