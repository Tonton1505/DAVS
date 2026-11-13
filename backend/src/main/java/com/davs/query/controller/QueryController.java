package com.davs.query.controller;

import com.davs.query.dto.QueryResult;
import com.davs.query.dto.QuerySpec;
import com.davs.query.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/query")
@RequiredArgsConstructor
@Tag(name = "Query Engine", description = "UC15 - Trình truy vấn dữ liệu theo QuerySpec phục vụ dựng biểu đồ")
public class QueryController {

    private final QueryService queryService;

    @PostMapping
    @Operation(summary = "Thực thi truy vấn dữ liệu từ QuerySpec (cột, hàng, bộ lọc, tổng hợp)")
    public ResponseEntity<QueryResult> executeQuery(@Valid @RequestBody QuerySpec querySpec) {
        QueryResult result = queryService.executeQuery(querySpec);
        return ResponseEntity.ok(result);
    }
}
