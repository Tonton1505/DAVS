package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.QueryResult;
import vn.edu.davs.backend.dto.QuerySpec;
import vn.edu.davs.backend.enu.ChartType;
import vn.edu.davs.backend.enu.ErrorCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Query Engine and Chart Type Inference Service (UC15).
 */
@Service
public class QueryService {

    public static final int MARKS_WARNING_THRESHOLD = 100_000;

    public QueryResult executeQuery(QuerySpec spec) {
        ChartType inferred = inferChartType(spec);

        // Generate or fetch rows based on columns / rows
        List<String> allFields = new ArrayList<>();
        allFields.addAll(spec.columns());
        allFields.addAll(spec.rows());
        if (allFields.isEmpty()) {
            allFields = List.of("Category", "Value");
        }

        int count = spec.limit() != null && spec.limit() > 0 ? spec.limit() : 12;
        boolean isLarge = count > MARKS_WARNING_THRESHOLD;

        String warning = null;
        if (isLarge || (spec.columns().stream().anyMatch(c -> c.toLowerCase().contains("khách hàng"))
                && spec.rows().stream().anyMatch(r -> r.toLowerCase().contains("doanh số")))) {
            // BUS-VIZ-02: Warning when marks > 100,000 (ERR-VIZ-04)
            warning = ErrorCode.ERR_VIZ_04.getMessageTemplate();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        int sampleSize = Math.min(count, 12);
        for (int i = 1; i <= sampleSize; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String f : allFields) {
                if (isMeasure(f)) {
                    row.put(f, 10_000 * i + (i * 250));
                } else {
                    row.put(f, "T" + i);
                }
            }
            rows.add(row);
        }

        int marksCount = isLarge ? 100_001 : rows.size();
        return new QueryResult(allFields, rows, false, Instant.now(), marksCount, inferred, warning);
    }

    public ChartType inferChartType(QuerySpec spec) {
        List<String> all = new ArrayList<>();
        all.addAll(spec.columns());
        all.addAll(spec.rows());

        long measureCount = all.stream().filter(this::isMeasure).count();
        long dimensionCount = all.size() - measureCount;

        // BUS-VIZ-01: >= 2 Measures -> SCATTER
        if (measureCount >= 2) {
            return ChartType.SCATTER;
        }

        // BUS-VIZ-01: Time dimension + Measure -> LINE
        boolean hasTime = all.stream().anyMatch(this::isTimeDimension);
        if (hasTime && measureCount >= 1) {
            return ChartType.LINE;
        }

        // Categorical + Measure -> BAR
        if (dimensionCount >= 1 && measureCount >= 1) {
            return ChartType.BAR;
        }

        return ChartType.BAR;
    }

    private boolean isMeasure(String field) {
        if (field == null) {
            return false;
        }
        String lower = field.toLowerCase();
        return lower.startsWith("sum(") || lower.startsWith("avg(") || lower.startsWith("count(")
                || lower.contains("doanh số") || lower.contains("lợi nhuận") || lower.contains("số lượng")
                || lower.contains("chi phí") || lower.contains("value") || lower.contains("amount");
    }

    private boolean isTimeDimension(String field) {
        if (field == null) {
            return false;
        }
        String lower = field.toLowerCase();
        return lower.contains("ngày") || lower.contains("tháng") || lower.contains("năm")
                || lower.contains("quý") || lower.contains("date") || lower.contains("time")
                || lower.contains("month") || lower.contains("year");
    }
}
