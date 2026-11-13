package com.davs.datasource.controller;

import com.davs.datasource.dto.DataSourceSummaryDto;
import com.davs.datasource.dto.DependentWorkbookDto;
import com.davs.datasource.dto.RefreshRunDto;
import com.davs.datasource.dto.RefreshScheduleDto;
import com.davs.datasource.dto.UpdateConnectionRequest;
import com.davs.datasource.dto.UpdateModeRequest;
import com.davs.datasource.service.DataSourceManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/datasources")
@RequiredArgsConstructor
@Tag(name = "Data Source Management", description = "UC14 - Quản lý nguồn dữ liệu, chế độ Live/Extract và lịch làm mới")
public class DataSourceController {

    private final DataSourceManageService dataSourceManageService;

    @GetMapping
    @Operation(summary = "Xem danh sách nguồn dữ liệu quản trị")
    public ResponseEntity<List<DataSourceSummaryDto>> listDataSources(
            @RequestParam(name = "managed", required = false, defaultValue = "true") boolean managed
    ) {
        List<DataSourceSummaryDto> list = dataSourceManageService.listManagedDataSources();
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{id}/mode")
    @Operation(summary = "Đổi chế độ kết nối Live <-> Extract")
    public ResponseEntity<DataSourceSummaryDto> updateMode(
            @PathVariable("id") UUID datasourceId,
            @Valid @RequestBody UpdateModeRequest request
    ) {
        DataSourceSummaryDto updated = dataSourceManageService.updateMode(datasourceId, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/refresh-schedule")
    @Operation(summary = "Lấy cấu hình lịch làm mới extract")
    public ResponseEntity<RefreshScheduleDto> getRefreshSchedule(@PathVariable("id") UUID datasourceId) {
        RefreshScheduleDto schedule = dataSourceManageService.getRefreshSchedule(datasourceId);
        return ResponseEntity.ok(schedule);
    }

    @PutMapping("/{id}/refresh-schedule")
    @Operation(summary = "Cập nhật lịch làm mới extract (tần suất, giờ chạy, múi giờ)")
    public ResponseEntity<RefreshScheduleDto> setRefreshSchedule(
            @PathVariable("id") UUID datasourceId,
            @Valid @RequestBody RefreshScheduleDto scheduleDto
    ) {
        RefreshScheduleDto saved = dataSourceManageService.setRefreshSchedule(datasourceId, scheduleDto);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{id}/refresh")
    @Operation(summary = "Kích hoạt làm mới extract ngay lập tức (202 Accepted)")
    public ResponseEntity<RefreshRunDto> triggerRefresh(@PathVariable("id") UUID datasourceId) {
        RefreshRunDto run = dataSourceManageService.triggerRefresh(datasourceId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(run);
    }

    @GetMapping("/{id}/refresh-runs")
    @Operation(summary = "Xem lịch sử các lần chạy làm mới extract")
    public ResponseEntity<List<RefreshRunDto>> getRefreshRuns(@PathVariable("id") UUID datasourceId) {
        List<RefreshRunDto> runs = dataSourceManageService.getRefreshRuns(datasourceId);
        return ResponseEntity.ok(runs);
    }

    @PutMapping("/{id}/connection")
    @Operation(summary = "Chỉnh sửa hoặc cập nhật cấu hình kết nối")
    public ResponseEntity<DataSourceSummaryDto> updateConnection(
            @PathVariable("id") UUID datasourceId,
            @Valid @RequestBody UpdateConnectionRequest request
    ) {
        DataSourceSummaryDto updated = dataSourceManageService.updateConnection(datasourceId, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/dependents")
    @Operation(summary = "Liệt kê các workbook phụ thuộc vào nguồn dữ liệu này")
    public ResponseEntity<List<DependentWorkbookDto>> getDependents(@PathVariable("id") UUID datasourceId) {
        List<DependentWorkbookDto> dependents = dataSourceManageService.getDependentWorkbooks(datasourceId);
        return ResponseEntity.ok(dependents);
    }
}
