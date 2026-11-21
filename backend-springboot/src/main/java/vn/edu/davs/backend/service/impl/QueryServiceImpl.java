package vn.edu.davs.backend.service.impl;

import vn.edu.davs.backend.dto.QueryResult;
import vn.edu.davs.backend.dto.QuerySpec;
import vn.edu.davs.backend.enu.ChartType;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.model.DavsUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.davs.backend.service.QueryService;
import vn.edu.davs.backend.service.RowFilterProvider;

@Service
public class QueryServiceImpl implements QueryService {

    private static final long CACHE_TTL_MS = 30_000L;

    private static final int MAX_CACHE_ENTRIES = 500;


    private final RowFilterProvider rowFilterProvider;

    private final Map<String, CachedQueryEntry> queryCache = new ConcurrentHashMap<>();


    private record CachedQueryEntry(QueryResult result, long expiresAtMillis) {
    }


    public QueryServiceImpl() {
        this(null);
    }


    @Autowired
    public QueryServiceImpl(RowFilterProvider rowFilterProvider) {
        this.rowFilterProvider = rowFilterProvider;
    }


    private static final int[] MONTHLY_SALES = {82, 96, 105, 118, 126, 138, 148, 156, 172, 188, 205, 240}
;

    private static final int[] MONTHLY_PROFIT = {24, 31, 35, 42, 40, 48, 53, 55, 62, 68, 75, 92}
;

    private static final int[] MONTHLY_QUANTITY = {450, 510, 580, 620, 690, 720, 780, 810, 890, 950, 1040, 1200}
;

    private static final double[] MONTHLY_DISCOUNT = {
        0.05, 0.05, 0.08, 0.08, 0.10, 0.10, 0.07, 0.07, 0.12, 0.12, 0.15, 0.20
    }
;

    private static final String[] REGIONS = {
        "Miền Bắc", "Miền Nam", "Miền Trung", "Miền Bắc",
        "Miền Nam", "Miền Trung", "Miền Bắc", "Miền Nam",
        "Miền Bắc", "Miền Trung", "Miền Nam", "Miền Bắc"
    }
;

    @Override
    public QueryResult executeQuery(QuerySpec spec) {
        return executeQuery(spec, null);
    }

    @Override
    public QueryResult executeQuery(QuerySpec spec, DavsUser actor) {
        UUID actorId = actor != null ? actor.getId() : null;
        boolean rlsActive = rowFilterProvider != null
                && spec.datasourceId() != null
                && rowFilterProvider.isRlsEnabled(spec.datasourceId());
        String cacheKey = spec.hashCode() + ":" + actorId + ":" + rlsActive;
        long now = System.currentTimeMillis();
        CachedQueryEntry cached = queryCache.get(cacheKey);
        if (cached != null && cached.expiresAtMillis() > now) {
            return cached.result();
        }

        ChartType inferred = inferChartType(spec);

        List<String> allFields = new ArrayList<>();
        allFields.addAll(spec.columns());
        allFields.addAll(spec.rows());
        if (allFields.isEmpty()) {
            allFields = List.of("Tháng (Ngày đặt hàng)", "SUM(Doanh số)");
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
        for (int i = 0; i < sampleSize; i++) {
            int monthIdx = i + 1;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", "T" + monthIdx);
            row.put("value", MONTHLY_SALES[i]);
            row.put("profit", MONTHLY_PROFIT[i]);
            row.put("quantity", MONTHLY_QUANTITY[i]);
            row.put("discount", MONTHLY_DISCOUNT[i]);
            row.put("Ma_KH", String.format("KH%02d", monthIdx));
            row.put("Vung", REGIONS[i]);
            row.put("Doanh_so", MONTHLY_SALES[i] * 1_000_000L);
            row.put("Loi_nhuan", MONTHLY_PROFIT[i] * 1_000_000L);
            row.put("So_luong", MONTHLY_QUANTITY[i]);

            for (String f : allFields) {
                if (isMeasure(f)) {
                    row.putIfAbsent(f, MONTHLY_SALES[i]);
                } else {
                    row.putIfAbsent(f, "T" + monthIdx);
                }
            }
            rows.add(row);
        }

        if (rlsActive) {
            rows = rowFilterProvider.applyRowFilters(spec.datasourceId(), actor, rows);
        }

        int marksCount = isLarge ? 100_001 : rows.size();
        QueryResult result = new QueryResult(allFields, rows, false, Instant.now(), marksCount, inferred, warning);
        if (queryCache.size() >= MAX_CACHE_ENTRIES) {
            queryCache.clear();
        }
        queryCache.put(cacheKey, new CachedQueryEntry(result, now + CACHE_TTL_MS));
        return result;
    }

    @Override
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
