package com.davs.query.service.impl;

import com.davs.common.error.ErrorCode;
import com.davs.datasource.domain.DataType;
import com.davs.datasource.domain.FieldRole;
import com.davs.datasource.dto.FieldDto;
import com.davs.query.dto.QueryResult;
import com.davs.query.dto.QuerySpec;
import com.davs.query.service.QueryService;
import com.davs.viz.domain.ChartType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QueryServiceImpl implements QueryService {

    private static final long MAX_RECOMMENDED_MARKS = 100000L; // BUS-VIZ-02

    @Override
    public QueryResult executeQuery(QuerySpec spec) {
        List<FieldDto> cols = spec.getColumns() != null ? spec.getColumns() : List.of();
        List<FieldDto> rows = spec.getRows() != null ? spec.getRows() : List.of();

        ChartType recommended = inferChartType(cols, rows);

        List<String> columnNames = new ArrayList<>();
        cols.forEach(c -> columnNames.add(c.getAlias() != null ? c.getAlias() : c.getName()));
        rows.forEach(r -> columnNames.add(r.getAlias() != null ? r.getAlias() : r.getName()));

        int rowCount = Math.min(Math.max(spec.getLimit(), 1), 1000);
        List<Map<String, Object>> dataRows = new ArrayList<>();

        for (int i = 1; i <= rowCount; i++) {
            Map<String, Object> row = new HashMap<>();
            for (FieldDto col : cols) {
                row.put(col.getName(), sampleVal(col, i));
            }
            for (FieldDto r : rows) {
                row.put(r.getName(), sampleVal(r, i));
            }
            dataRows.add(row);
        }

        long marks = (long) dataRows.size() * Math.max(1, cols.size());
        String warning = null;
        if (marks > MAX_RECOMMENDED_MARKS) {
            warning = ErrorCode.ERR_VIZ_04.getMessageTemplate();
        }

        return QueryResult.builder()
                .columns(columnNames)
                .rows(dataRows)
                .totalMarks(marks)
                .truncated(false)
                .dataAsOf(Instant.now())
                .recommendedChartType(recommended)
                .warning(warning)
                .build();
    }

    @Override
    public ChartType inferChartType(List<FieldDto> columns, List<FieldDto> rows) {
        List<FieldDto> all = new ArrayList<>();
        if (columns != null) {
            all.addAll(columns);
        }
        if (rows != null) {
            all.addAll(rows);
        }

        long timeDimensions = all.stream()
                .filter(f -> (f.getDataType() == DataType.DATE || f.getDataType() == DataType.DATETIME))
                .count();

        long measures = all.stream()
                .filter(f -> f.getRole() == FieldRole.MEASURE || (f.getDataType() == DataType.NUMBER && !isId(f.getName())))
                .count();

        long categoricalDimensions = all.stream()
                .filter(f -> f.getRole() == FieldRole.DIMENSION && f.getDataType() == DataType.STRING)
                .count();

        // BUS-VIZ-01 inference rules
        if (timeDimensions >= 1 && measures >= 1) {
            return ChartType.LINE;
        }
        if (measures >= 2 && timeDimensions == 0 && categoricalDimensions == 0) {
            return ChartType.SCATTER;
        }
        if (categoricalDimensions >= 1 && measures >= 1) {
            return ChartType.BAR;
        }
        return ChartType.BAR;
    }

    private boolean isId(String name) {
        if (name == null) {
            return false;
        }
        String clean = name.toLowerCase().replaceAll("[^a-z0-9]", "");
        return clean.endsWith("id") || clean.startsWith("ma");
    }

    private Object sampleVal(FieldDto f, int idx) {
        if (f.getDataType() == DataType.NUMBER) {
            return idx * 100;
        }
        if (f.getDataType() == DataType.DATE) {
            return "2026-11-" + String.format("%02d", (idx % 28) + 1);
        }
        return f.getName() + " " + idx;
    }
}
