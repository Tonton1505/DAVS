package com.davs.datasource.service.impl;

import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.datasource.domain.DataModelEntity;
import com.davs.datasource.domain.DataSourceEntity;
import com.davs.datasource.domain.DataType;
import com.davs.datasource.domain.FieldRole;
import com.davs.datasource.domain.JoinType;
import com.davs.datasource.dto.DataModelDto;
import com.davs.datasource.dto.FieldDto;
import com.davs.datasource.dto.PreviewModelRequest;
import com.davs.datasource.dto.PreviewModelResponse;
import com.davs.datasource.dto.RelationshipDto;
import com.davs.datasource.dto.SuggestRelationshipsRequest;
import com.davs.datasource.dto.TableDto;
import com.davs.datasource.repository.DataModelRepository;
import com.davs.datasource.repository.DataSourceRepository;
import com.davs.datasource.service.DataModelService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataModelServiceImpl implements DataModelService {

    private final DataModelRepository dataModelRepository;
    private final DataSourceRepository dataSourceRepository;

    @Override
    @Transactional(readOnly = true)
    public DataModelDto getModel(UUID datasourceId) {
        Optional<DataModelEntity> entityOpt = dataModelRepository.findByDatasourceId(datasourceId);
        if (entityOpt.isPresent()) {
            return toDto(entityOpt.get());
        }

        // Return empty initial model if not yet configured
        return DataModelDto.builder()
                .datasourceId(datasourceId)
                .tables(new ArrayList<>())
                .relationships(new ArrayList<>())
                .fields(new ArrayList<>())
                .version(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Override
    @Transactional
    public DataModelDto saveModel(UUID datasourceId, DataModelDto modelDto) {
        // Verify datasource exists if present in database
        Optional<DataSourceEntity> dsOpt = dataSourceRepository.findByIdAndDeletedAtIsNull(datasourceId);
        if (dsOpt.isEmpty()) {
            log.debug("Datasource {} not yet in database; proceeding with standalone model save", datasourceId);
        }

        // 1. Validate relationship keys and cross-source blend rules
        validateModelRules(modelDto);

        // 2. Apply BUS-DAT-08: Number defaults to Measure, except ID/Code fields
        applyDefaultFieldRoles(modelDto.getFields());

        // 3. Persist entity
        Optional<DataModelEntity> existingOpt = dataModelRepository.findByDatasourceId(datasourceId);
        DataModelEntity entity;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            entity.setVersion(entity.getVersion() != null ? entity.getVersion() + 1 : 2);
        } else {
            entity = DataModelEntity.builder()
                    .datasourceId(datasourceId)
                    .createdAt(Instant.now())
                    .version(1)
                    .build();
        }

        entity.setTables(modelDto.getTables());
        entity.setRelationships(modelDto.getRelationships());
        entity.setFields(modelDto.getFields());
        entity.setUpdatedAt(Instant.now());

        DataModelEntity saved = dataModelRepository.save(entity);
        return toDto(saved);
    }

    @Override
    public List<RelationshipDto> suggestRelationships(UUID datasourceId, SuggestRelationshipsRequest request) {
        List<RelationshipDto> suggestions = new ArrayList<>();
        if (request == null || request.getTables() == null || request.getFields() == null) {
            return suggestions;
        }

        List<TableDto> tables = request.getTables();
        List<FieldDto> fields = request.getFields();

        // Group fields by table
        Map<String, List<FieldDto>> fieldsByTable = new HashMap<>();
        for (FieldDto field : fields) {
            fieldsByTable.computeIfAbsent(field.getTable(), k -> new ArrayList<>()).add(field);
        }

        // Compare each pair of tables
        for (int i = 0; i < tables.size(); i++) {
            for (int j = i + 1; j < tables.size(); j++) {
                TableDto tableA = tables.get(i);
                TableDto tableB = tables.get(j);

                List<FieldDto> fieldsA = fieldsByTable.getOrDefault(tableA.getName(), List.of());
                List<FieldDto> fieldsB = fieldsByTable.getOrDefault(tableB.getName(), List.of());

                for (FieldDto fA : fieldsA) {
                    for (FieldDto fB : fieldsB) {
                        if (isMatchingFieldName(fA.getName(), fB.getName())) {
                            // Check compatibility: if compatible, suggest relationship
                            if (fA.getDataType().isCompatibleWith(fB.getDataType())) {
                                suggestions.add(RelationshipDto.builder()
                                        .id(UUID.randomUUID().toString())
                                        .fromTable(tableA.getName())
                                        .fromField(fA.getName())
                                        .toTable(tableB.getName())
                                        .toField(fB.getName())
                                        .joinType(JoinType.RELATIONSHIP) // BUS-DAT-07: default Relationship
                                        .fromSourceId(tableA.getSourceId())
                                        .toSourceId(tableB.getSourceId())
                                        .build());
                            }
                        }
                    }
                }
            }
        }

        return suggestions;
    }

    @Override
    public PreviewModelResponse previewModel(UUID datasourceId, PreviewModelRequest request) {
        DataModelDto model = request.getModel();
        validateModelRules(model);

        List<FieldDto> activeFields = model.getFields().stream()
                .filter(f -> !f.isHidden())
                .toList();

        List<String> columns = activeFields.stream()
                .map(f -> f.getAlias() != null && !f.getAlias().isBlank() ? f.getAlias() : f.getName())
                .toList();

        int limit = Math.min(Math.max(request.getLimit(), 1), 500);
        List<Map<String, Object>> sampleRows = new ArrayList<>();
        Map<String, Long> nullCounts = new HashMap<>();
        for (String col : columns) {
            nullCounts.put(col, 0L);
        }

        int affectedRows = 0;
        // Generate simulated sample data reflecting types & relations
        for (int rowIdx = 1; rowIdx <= limit; rowIdx++) {
            Map<String, Object> row = new HashMap<>();
            boolean rowHasConvertedNull = false;

            for (FieldDto field : activeFields) {
                String colName = field.getAlias() != null && !fBlank(field.getAlias()) ? field.getAlias() : field.getName();
                Object sampleValue = generateSampleValue(field, rowIdx);

                if (sampleValue == null) {
                    nullCounts.put(colName, nullCounts.getOrDefault(colName, 0L) + 1);
                    rowHasConvertedNull = true;
                }
                row.put(colName, sampleValue);
            }

            if (rowHasConvertedNull) {
                affectedRows++;
            }
            sampleRows.add(row);
        }

        List<String> warnings = new ArrayList<>();
        if (affectedRows > 0) {
            warnings.add(String.format("Phát hiện %d dòng có giá trị null khi áp dụng mô hình dữ liệu hiện tại.", affectedRows));
        }

        return PreviewModelResponse.builder()
                .columns(columns)
                .rows(sampleRows)
                .totalRows(sampleRows.size())
                .affectedRows(affectedRows)
                .nullCountByField(nullCounts)
                .warnings(warnings)
                .build();
    }

    @Override
    public void applyDefaultFieldRoles(List<FieldDto> fields) {
        if (fields == null) {
            return;
        }
        for (FieldDto field : fields) {
            if (field.getRole() == null) {
                if (field.getDataType() == DataType.NUMBER) {
                    field.setRole(isIdField(field.getName()) ? FieldRole.DIMENSION : FieldRole.MEASURE);
                } else {
                    field.setRole(FieldRole.DIMENSION);
                }
            } else if (field.getDataType() == DataType.NUMBER && isIdField(field.getName())) {
                field.setRole(FieldRole.DIMENSION);
            }
        }
    }

    private void validateModelRules(DataModelDto modelDto) {
        if (modelDto == null || modelDto.getRelationships() == null) {
            return;
        }

        Map<String, FieldDto> fieldLookup = new HashMap<>();
        for (FieldDto f : modelDto.getFields()) {
            String key = (f.getTable() + "." + f.getName()).toLowerCase();
            fieldLookup.put(key, f);
        }

        for (RelationshipDto rel : modelDto.getRelationships()) {
            String fromKey = (rel.getFromTable() + "." + rel.getFromField()).toLowerCase();
            String toKey = (rel.getToTable() + "." + rel.getToField()).toLowerCase();

            FieldDto fromField = fieldLookup.get(fromKey);
            FieldDto toField = fieldLookup.get(toKey);

            // [6a] Validate key type compatibility
            if (fromField != null && toField != null) {
                if (!fromField.getDataType().isCompatibleWith(toField.getDataType())) {
                    log.warn("ERR-DAT-06: Incompatible relationship keys: {} ({}) <-> {} ({})",
                            fromKey, fromField.getDataType(), toKey, toField.getDataType());
                    throw new DavsException(ErrorCode.ERR_DAT_06);
                }
            }

            // BUS-DAT-09: Blend only for tables from different data sources
            if (rel.getJoinType() == JoinType.BLEND) {
                UUID fromSource = rel.getFromSourceId();
                UUID toSource = rel.getToSourceId();

                if (fromSource != null && toSource != null && fromSource.equals(toSource)) {
                    log.warn("BUS-DAT-09 violated: Blend applied between tables in same data source {}", fromSource);
                    throw new DavsException(
                            ErrorCode.ERR_DAT_06,
                            "Blend (Trộn) chỉ áp dụng cho các bảng thuộc nguồn dữ liệu khác nhau."
                    );
                }
            }

            // BUS-DAT-07: Default to Relationship
            if (rel.getJoinType() == null) {
                rel.setJoinType(JoinType.RELATIONSHIP);
            }
        }
    }

    private boolean isMatchingFieldName(String nameA, String nameB) {
        if (nameA == null || nameB == null) {
            return false;
        }
        String cleanA = nameA.toLowerCase().replaceAll("[^a-z0-9]", "");
        String cleanB = nameB.toLowerCase().replaceAll("[^a-z0-9]", "");
        return cleanA.equals(cleanB);
    }

    private boolean isIdField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String clean = fieldName.toLowerCase().replaceAll("[^a-z0-9]", "");
        return clean.endsWith("id")
                || clean.startsWith("ma")
                || clean.contains("code")
                || clean.equals("id");
    }

    private Object generateSampleValue(FieldDto field, int rowIdx) {
        return switch (field.getDataType()) {
            case NUMBER -> field.getRole() == FieldRole.MEASURE ? 1000 + (rowIdx * 50) : rowIdx;
            case DATE -> "2026-11-" + String.format("%02d", (rowIdx % 28) + 1);
            case DATETIME -> "2026-11-" + String.format("%02d", (rowIdx % 28) + 1) + "T10:00:00Z";
            case BOOLEAN -> (rowIdx % 2 == 0);
            case GEOGRAPHIC -> "Hà Nội";
            default -> field.getName() + " #" + rowIdx;
        };
    }

    private boolean fBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private DataModelDto toDto(DataModelEntity entity) {
        return DataModelDto.builder()
                .id(entity.getId())
                .datasourceId(entity.getDatasourceId())
                .tables(entity.getTables() != null ? entity.getTables() : new ArrayList<>())
                .relationships(entity.getRelationships() != null ? entity.getRelationships() : new ArrayList<>())
                .fields(entity.getFields() != null ? entity.getFields() : new ArrayList<>())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
