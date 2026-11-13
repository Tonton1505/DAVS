package com.davs.datasource.controller;

import com.davs.datasource.dto.DataModelDto;
import com.davs.datasource.dto.PreviewModelRequest;
import com.davs.datasource.dto.PreviewModelResponse;
import com.davs.datasource.dto.RelationshipDto;
import com.davs.datasource.dto.SuggestRelationshipsRequest;
import com.davs.datasource.service.DataModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/datasources")
@RequiredArgsConstructor
@Tag(name = "Data Model", description = "UC13 - Mô hình hóa dữ liệu (Relationships, Joins, Blends, Fields)")
public class DataModelController {

    private final DataModelService dataModelService;

    @GetMapping("/{id}/model")
    @Operation(summary = "Lấy cấu hình mô hình dữ liệu của nguồn dữ liệu")
    public ResponseEntity<DataModelDto> getModel(@PathVariable("id") UUID datasourceId) {
        DataModelDto model = dataModelService.getModel(datasourceId);
        return ResponseEntity.ok(model);
    }

    @PutMapping("/{id}/model")
    @Operation(summary = "Lưu hoặc cập nhật cấu hình mô hình dữ liệu (bảng, quan hệ, trường)")
    public ResponseEntity<DataModelDto> saveModel(
            @PathVariable("id") UUID datasourceId,
            @Valid @RequestBody DataModelDto modelDto
    ) {
        DataModelDto saved = dataModelService.saveModel(datasourceId, modelDto);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{id}/model/suggest-relationships")
    @Operation(summary = "Tự động gợi ý mối quan hệ dựa trên tên và kiểu dữ liệu của các trường")
    public ResponseEntity<List<RelationshipDto>> suggestRelationships(
            @PathVariable("id") UUID datasourceId,
            @Valid @RequestBody SuggestRelationshipsRequest request
    ) {
        List<RelationshipDto> suggestions = dataModelService.suggestRelationships(datasourceId, request);
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping("/{id}/model/preview")
    @Operation(summary = "Xem trước kết quả dữ liệu sau khi áp dụng mô hình và liên kết")
    public ResponseEntity<PreviewModelResponse> previewModel(
            @PathVariable("id") UUID datasourceId,
            @Valid @RequestBody PreviewModelRequest request
    ) {
        PreviewModelResponse preview = dataModelService.previewModel(datasourceId, request);
        return ResponseEntity.ok(preview);
    }
}
