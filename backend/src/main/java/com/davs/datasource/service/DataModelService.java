package com.davs.datasource.service;

import com.davs.datasource.dto.DataModelDto;
import com.davs.datasource.dto.FieldDto;
import com.davs.datasource.dto.PreviewModelRequest;
import com.davs.datasource.dto.PreviewModelResponse;
import com.davs.datasource.dto.RelationshipDto;
import com.davs.datasource.dto.SuggestRelationshipsRequest;
import java.util.List;
import java.util.UUID;

public interface DataModelService {

    DataModelDto getModel(UUID datasourceId);

    DataModelDto saveModel(UUID datasourceId, DataModelDto modelDto);

    List<RelationshipDto> suggestRelationships(UUID datasourceId, SuggestRelationshipsRequest request);

    PreviewModelResponse previewModel(UUID datasourceId, PreviewModelRequest request);

    void applyDefaultFieldRoles(List<FieldDto> fields);
}
