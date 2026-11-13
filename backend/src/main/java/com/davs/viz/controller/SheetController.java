package com.davs.viz.controller;

import com.davs.viz.dto.SheetDto;
import com.davs.viz.service.SheetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workbooks/{workbookId}/sheets")
@RequiredArgsConstructor
@Tag(name = "Worksheet Builder", description = "UC15 - Quản lý worksheet và cấu hình biểu đồ trong workbook")
public class SheetController {

    private final SheetService sheetService;

    @GetMapping
    @Operation(summary = "Lấy danh sách sheet trong workbook")
    public ResponseEntity<List<SheetDto>> getSheets(@PathVariable("workbookId") UUID workbookId) {
        List<SheetDto> sheets = sheetService.getSheets(workbookId);
        return ResponseEntity.ok(sheets);
    }

    @GetMapping("/{sheetId}")
    @Operation(summary = "Lấy chi tiết cấu hình của một sheet")
    public ResponseEntity<SheetDto> getSheet(
            @PathVariable("workbookId") UUID workbookId,
            @PathVariable("sheetId") UUID sheetId
    ) {
        SheetDto sheet = sheetService.getSheet(workbookId, sheetId);
        return ResponseEntity.ok(sheet);
    }

    @PostMapping
    @Operation(summary = "Tạo mới một sheet trong workbook")
    public ResponseEntity<SheetDto> createSheet(
            @PathVariable("workbookId") UUID workbookId,
            @Valid @RequestBody SheetDto sheetDto
    ) {
        SheetDto created = sheetService.createSheet(workbookId, sheetDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{sheetId}")
    @Operation(summary = "Cập nhật cấu hình sheet (kệ Cột, Hàng, thẻ Marks, loại biểu đồ)")
    public ResponseEntity<SheetDto> updateSheet(
            @PathVariable("workbookId") UUID workbookId,
            @PathVariable("sheetId") UUID sheetId,
            @Valid @RequestBody SheetDto sheetDto
    ) {
        SheetDto updated = sheetService.updateSheet(workbookId, sheetId, sheetDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{sheetId}")
    @Operation(summary = "Xóa một sheet khỏi workbook")
    public ResponseEntity<Void> deleteSheet(
            @PathVariable("workbookId") UUID workbookId,
            @PathVariable("sheetId") UUID sheetId
    ) {
        sheetService.deleteSheet(workbookId, sheetId);
        return ResponseEntity.noContent().build();
    }
}
