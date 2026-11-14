package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.JdbcDatasourceRepository;
import vn.edu.davs.backend.dao.JdbcPrepRepository;
import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.DiscoveredTableDto;
import vn.edu.davs.backend.dto.PrepFlowDto;
import vn.edu.davs.backend.dto.PrepPreviewRequest;
import vn.edu.davs.backend.dto.PrepPreviewResponse;
import vn.edu.davs.backend.dto.PrepRunDto;
import vn.edu.davs.backend.dto.SavePrepFlowRequest;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.model.DavsUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core UC12 business service for Data Preparation &amp; Cleaning flows:
 * supports FILTER, RENAME, DROP, REPLACE_NULL, DEDUPE, CAST, JOIN, UNION, PIVOT, AGGREGATE,
 * 1,000-row preview with column profiling (BUS-DAT-06), step lineage (BUS-DAT-05),
 * and read-only source isolation (BUS-DAT-04).
 */
@Service
@RequiredArgsConstructor
public class DataPrepService {

    public static final String PREP_RUN_JOB_TYPE = "PREP_RUN";
    private static final int PREVIEW_ROW_LIMIT = 1000;

    private final JdbcPrepRepository prepRepository;
    private final JdbcDatasourceRepository datasourceRepository;
    private final JobQueueService jobQueueService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<PrepFlowDto> listFlows(UUID datasourceId) {
        return prepRepository.listFlows(datasourceId);
    }

    @Transactional(readOnly = true)
    public PrepFlowDto getFlow(UUID flowId) {
        return prepRepository.findFlowById(flowId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_SHR_01, flowId.toString()));
    }

    @Transactional
    public PrepFlowDto createFlow(SavePrepFlowRequest request, DavsUser actor) {
        String name = (request != null && request.name() != null && !request.name().isBlank())
                ? request.name().trim()
                : "Luồng làm sạch dữ liệu";
        List<PrepFlowDto.PrepStepDto> steps = request != null && request.steps() != null
                ? request.steps()
                : List.of();

        validateSteps(steps);
        ExecutionOutcome preview = evaluateSteps(request != null ? request.datasourceId() : null, steps, steps.size());

        UUID flowId = prepRepository.insertFlow(
                request != null ? request.datasourceId() : null,
                name,
                request != null ? request.description() : null,
                steps,
                preview.lineage(),
                actor.getId(),
                request != null && Boolean.TRUE.equals(request.published())
        );

        PrepFlowDto created = getFlow(flowId);
        auditService.log(AuditEvent.of("PREP_FLOW_CREATE", actor.getId(), actor.getEmail(), "PREP_FLOW", flowId.toString())
                .withStates(null, Map.of("name", created.name(), "stepCount", created.steps().size())));
        return created;
    }

    @Transactional
    public PrepFlowDto updateFlow(UUID flowId, SavePrepFlowRequest request, DavsUser actor) {
        PrepFlowDto existing = getFlow(flowId);
        String name = (request.name() != null && !request.name().isBlank())
                ? request.name().trim()
                : existing.name();
        String desc = request.description() != null ? request.description() : existing.description();
        UUID dsId = request.datasourceId() != null ? request.datasourceId() : existing.datasourceId();
        List<PrepFlowDto.PrepStepDto> steps = request.steps() != null ? request.steps() : existing.steps();
        boolean published = request.published() != null ? request.published() : existing.published();

        validateSteps(steps);
        ExecutionOutcome preview = evaluateSteps(dsId, steps, steps.size());

        prepRepository.updateFlow(flowId, dsId, name, desc, steps, preview.lineage(), published);
        PrepFlowDto updated = getFlow(flowId);

        auditService.log(AuditEvent.of("PREP_FLOW_UPDATE", actor.getId(), actor.getEmail(), "PREP_FLOW", flowId.toString())
                .withStates(Map.of("stepCount", existing.steps().size()), Map.of("stepCount", updated.steps().size())));
        return updated;
    }

    @Transactional(readOnly = true)
    public PrepPreviewResponse previewFlow(UUID flowId, PrepPreviewRequest request) {
        PrepFlowDto flow = getFlow(flowId);
        List<PrepFlowDto.PrepStepDto> steps = (request != null && request.steps() != null)
                ? request.steps()
                : flow.steps();
        int targetIndex = (request != null && request.stepIndex() != null)
                ? Math.min(steps.size(), Math.max(0, request.stepIndex() + 1))
                : steps.size();

        validateSteps(steps.subList(0, targetIndex));
        ExecutionOutcome outcome = evaluateSteps(flow.datasourceId(), steps, targetIndex);
        List<Map<String, Object>> limitedRows = outcome.rows().stream()
                .limit(PREVIEW_ROW_LIMIT)
                .toList();
        List<PrepPreviewResponse.ColumnProfileDto> profiles = buildColumnProfiles(limitedRows);

        return new PrepPreviewResponse(
                targetIndex - 1,
                outcome.inputRows(),
                limitedRows.size(),
                outcome.removedDuplicates(),
                outcome.replacedNulls(),
                profiles,
                outcome.lineage(),
                limitedRows
        );
    }

    @Transactional
    public PrepRunDto enqueueRun(UUID flowId, boolean simulateFailure, DavsUser actor) {
        PrepFlowDto flow = getFlow(flowId);
        validateSteps(flow.steps());

        UUID runId = prepRepository.insertRun(flowId, null, actor.getId());
        UUID jobId = jobQueueService.enqueue(
                PREP_RUN_JOB_TYPE,
                Map.of(
                        "runId", runId.toString(),
                        "flowId", flowId.toString(),
                        "simulateFailure", simulateFailure
                ),
                Instant.now(),
                1,
                actor.getId()
        );

        auditService.log(AuditEvent.of("PREP_FLOW_RUN_TRIGGER", actor.getId(), actor.getEmail(), "PREP_FLOW", flowId.toString())
                .withStates(null, Map.of("runId", runId.toString(), "jobId", jobId.toString())));

        return prepRepository.findRunById(runId)
                .orElseThrow(() -> new IllegalStateException("Prep run not found: " + runId));
    }

    @Transactional
    public PrepRunDto executeRun(UUID runId, boolean simulateFailure) {
        PrepRunDto run = prepRepository.findRunById(runId)
                .orElseThrow(() -> new IllegalStateException("Prep run not found: " + runId));
        PrepFlowDto flow = getFlow(run.flowId());

        prepRepository.markRunRunning(runId);

        boolean hasFailingStep = simulateFailure || flow.steps().stream().anyMatch(
                s -> Boolean.parseBoolean(String.valueOf(s.config().getOrDefault("failOnRun", false)))
        );
        if (hasFailingStep) {
            // [5a] ERR-DAT-05: Run failure preserves previous output/extract intact
            prepRepository.completeRunFailure(
                    runId,
                    ErrorCode.ERR_DAT_05.getCode(),
                    ErrorCode.ERR_DAT_05.getMessageTemplate()
            );
            return prepRepository.findRunById(runId).orElseThrow();
        }

        ExecutionOutcome outcome = evaluateSteps(flow.datasourceId(), flow.steps(), flow.steps().size());
        if (flow.datasourceId() != null) {
            // Write cleaned extract snapshot without mutating raw source ds_tables (BUS-DAT-04)
            datasourceRepository.upsertExtractSnapshot(
                    flow.datasourceId(),
                    outcome.rows().size(),
                    outcome.rows()
            );
        }

        prepRepository.completeRunSuccess(
                runId,
                flow.id(),
                outcome.inputRows(),
                outcome.rows().size(),
                outcome.removedDuplicates(),
                outcome.replacedNulls(),
                outcome.lineage()
        );
        return prepRepository.findRunById(runId).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<PrepRunDto> listRuns(UUID flowId) {
        getFlow(flowId);
        return prepRepository.listRuns(flowId);
    }

    private void validateSteps(List<PrepFlowDto.PrepStepDto> steps) {
        if (steps == null) {
            return;
        }
        for (PrepFlowDto.PrepStepDto step : steps) {
            String type = step.type() != null ? step.type().trim().toUpperCase(Locale.ROOT) : "";
            if ("JOIN".equals(type)) {
                String leftType = String.valueOf(step.config().getOrDefault("leftKeyType", "String"));
                String rightType = String.valueOf(step.config().getOrDefault("rightKeyType", "String"));
                // [4a] ERR-DAT-06: Incompatible join key types
                if (!DatasourceAndModelService.areTypesCompatible(leftType, rightType)) {
                    throw new DavsException(ErrorCode.ERR_DAT_06);
                }
            }
        }
    }

    private ExecutionOutcome evaluateSteps(
            UUID datasourceId,
            List<PrepFlowDto.PrepStepDto> steps,
            int maxStepExclusive
    ) {
        List<Map<String, Object>> currentRows = loadSourceRows(datasourceId);
        long initialRowCount = currentRows.size();
        long removedDuplicates = 0L;
        long replacedNulls = 0L;
        List<PrepFlowDto.LineageStepDto> lineage = new ArrayList<>();

        int limit = steps == null ? 0 : Math.min(steps.size(), maxStepExclusive);
        for (int idx = 0; idx < limit; idx++) {
            PrepFlowDto.PrepStepDto step = steps.get(idx);
            String type = step.type() != null ? step.type().trim().toUpperCase(Locale.ROOT) : "FILTER";
            Map<String, Object> cfg = step.config() != null ? step.config() : Map.of();
            long beforeCount = currentRows.size();

            switch (type) {
                case "FILTER" -> currentRows = applyFilter(currentRows, cfg);
                case "RENAME" -> currentRows = applyRename(currentRows, cfg);
                case "DROP" -> currentRows = applyDrop(currentRows, cfg);
                case "REPLACE_NULL" -> {
                    ReplaceNullOutcome rno = applyReplaceNull(currentRows, cfg);
                    currentRows = rno.rows();
                    replacedNulls += rno.replacedCount();
                }
                case "DEDUPE" -> {
                    List<Map<String, Object>> deduped = applyDedupe(currentRows, cfg);
                    removedDuplicates += Math.max(0L, beforeCount - deduped.size());
                    currentRows = deduped;
                }
                case "CAST" -> currentRows = applyCast(currentRows, cfg);
                case "JOIN" -> {
                    String leftKeyType = String.valueOf(cfg.getOrDefault("leftKeyType", "String"));
                    String rightKeyType = String.valueOf(cfg.getOrDefault("rightKeyType", "String"));
                    if (!DatasourceAndModelService.areTypesCompatible(leftKeyType, rightKeyType)) {
                        throw new DavsException(ErrorCode.ERR_DAT_06);
                    }
                    currentRows = applyJoin(currentRows, cfg);
                }
                case "UNION" -> currentRows = applyUnion(currentRows, cfg);
                case "PIVOT" -> currentRows = applyPivot(currentRows, cfg);
                case "AGGREGATE" -> currentRows = applyAggregate(currentRows, cfg);
                default -> {
                    // Keep rows unchanged for passthrough steps (e.g., INPUT/OUTPUT)
                }
            }

            lineage.add(new PrepFlowDto.LineageStepDto(
                    idx,
                    type,
                    step.name() != null && !step.name().isBlank() ? step.name() : type,
                    beforeCount,
                    currentRows.size()
            ));
        }

        return new ExecutionOutcome(
                initialRowCount,
                currentRows,
                removedDuplicates,
                replacedNulls,
                lineage
        );
    }

    private List<Map<String, Object>> loadSourceRows(UUID datasourceId) {
        if (datasourceId != null) {
            List<DiscoveredTableDto> tables = datasourceRepository.findTablesByDatasource(datasourceId);
            if (!tables.isEmpty() && !tables.get(0).previewRows().isEmpty()) {
                List<Map<String, Object>> cloned = new ArrayList<>();
                for (Map<String, Object> r : tables.get(0).previewRows()) {
                    cloned.add(new LinkedHashMap<>(r));
                }
                return cloned;
            }
        }
        // Default sample dataset when no datasource rows exist yet
        List<Map<String, Object>> defaults = new ArrayList<>();
        Map<String, Object> r1 = new LinkedHashMap<>();
        r1.put("Ma_DH", "DH001");
        r1.put("Vung", "Miền Bắc");
        r1.put("Doanh_So", 205000000L);
        r1.put("Loi_Nhuan", null);
        defaults.add(r1);

        Map<String, Object> r2 = new LinkedHashMap<>();
        r2.put("Ma_DH", "DH002");
        r2.put("Vung", "Miền Nam");
        r2.put("Doanh_So", 240000000L);
        r2.put("Loi_Nhuan", 42000000L);
        defaults.add(r2);

        Map<String, Object> r3 = new LinkedHashMap<>(r2);
        defaults.add(r3);
        return defaults;
    }

    private static List<Map<String, Object>> applyFilter(List<Map<String, Object>> rows, Map<String, Object> cfg) {
        String field = str(cfg.get("field"));
        String op = str(cfg.getOrDefault("operator", "NOT_NULL")).toUpperCase(Locale.ROOT);
        String value = str(cfg.get("value"));
        if (field.isEmpty()) {
            return rows;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object cell = row.get(field);
            boolean keep = switch (op) {
                case "NOT_NULL" -> cell != null && !String.valueOf(cell).isBlank();
                case "EQUALS", "=" -> cell != null && String.valueOf(cell).equalsIgnoreCase(value);
                case "NOT_EQUALS", "!=" -> cell == null || !String.valueOf(cell).equalsIgnoreCase(value);
                case "CONTAINS" -> cell != null
                        && String.valueOf(cell).toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
                case "GT", ">" -> compareNumeric(cell, value) > 0;
                case "GTE", ">=" -> compareNumeric(cell, value) >= 0;
                case "LT", "<" -> compareNumeric(cell, value) < 0;
                default -> true;
            };
            if (keep) {
                out.add(new LinkedHashMap<>(row));
            }
        }
        return out;
    }

    private static List<Map<String, Object>> applyRename(List<Map<String, Object>> rows, Map<String, Object> cfg) {
        String from = str(cfg.get("field"));
        String to = str(cfg.getOrDefault("newName", cfg.get("to")));
        if (from.isEmpty() || to.isEmpty() || from.equals(to)) {
            return rows;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> next = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : row.entrySet()) {
                next.put(e.getKey().equals(from) ? to : e.getKey(), e.getValue());
            }
            out.add(next);
        }
        return out;
    }

    private static List<Map<String, Object>> applyDrop(List<Map<String, Object>> rows, Map<String, Object> cfg) {
        String field = str(cfg.get("field"));
        if (field.isEmpty()) {
            return rows;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> next = new LinkedHashMap<>(row);
            next.remove(field);
            out.add(next);
        }
        return out;
    }

    private static ReplaceNullOutcome applyReplaceNull(List<Map<String, Object>> rows, Map<String, Object> cfg) {
        String field = str(cfg.get("field"));
        Object replacement = cfg.getOrDefault("replacement", cfg.getOrDefault("value", 0));
        long count = 0L;
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> next = new LinkedHashMap<>(row);
            if (!field.isEmpty()) {
                Object val = next.get(field);
                if (val == null || String.valueOf(val).isBlank()) {
                    next.put(field, replacement);
                    count++;
                }
            } else {
                for (Map.Entry<String, Object> e : next.entrySet()) {
                    if (e.getValue() == null || String.valueOf(e.getValue()).isBlank()) {
                        next.put(e.getKey(), replacement);
                        count++;
                    }
                }
            }
            out.add(next);
        }
        return new ReplaceNullOutcome(out, count);
    }

    private static List<Map<String, Object>> applyDedupe(List<Map<String, Object>> rows, Map<String, Object> cfg) {
        String keyField = str(cfg.get("field"));
        Set<Object> seen = new LinkedHashSet<>();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object key = keyField.isEmpty() ? row : row.get(keyField);
            if (seen.add(key)) {
                out.add(new LinkedHashMap<>(row));
            }
        }
        return out;
    }

    private static List<Map<String, Object>> applyCast(List<Map<String, Object>> rows, Map<String, Object> cfg) {
        String field = str(cfg.get("field"));
        String targetType = str(cfg.getOrDefault("targetType", "String")).toUpperCase(Locale.ROOT);
        if (field.isEmpty()) {
            return rows;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> next = new LinkedHashMap<>(row);
            Object val = next.get(field);
            if (val != null) {
                Object casted = switch (targetType) {
                    case "NUMBER", "LONG", "INT" -> {
                        try {
                            yield Double.valueOf(String.valueOf(val).trim()).longValue();
                        } catch (NumberFormatException ex) {
                            yield 0L;
                        }
                    }
                    case "BOOLEAN" -> Boolean.parseBoolean(String.valueOf(val).trim());
                    default -> String.valueOf(val);
                };
                next.put(field, casted);
            }
            out.add(next);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> applyJoin(List<Map<String, Object>> rows, Map<String, Object> cfg) {
        String leftKey = str(cfg.getOrDefault("leftKey", cfg.get("field")));
        String rightKey = str(cfg.getOrDefault("rightKey", leftKey));
        Object rightRowsObj = cfg.get("rightRows");
        if (!(rightRowsObj instanceof List<?> rightList) || leftKey.isEmpty()) {
            return rows;
        }
        Map<String, Map<String, Object>> lookup = new LinkedHashMap<>();
        for (Object item : rightList) {
            if (item instanceof Map<?, ?> m) {
                Object k = m.get(rightKey);
                if (k != null) {
                    lookup.put(String.valueOf(k), (Map<String, Object>) m);
                }
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> merged = new LinkedHashMap<>(row);
            Object k = row.get(leftKey);
            if (k != null && lookup.containsKey(String.valueOf(k))) {
                merged.putAll(lookup.get(String.valueOf(k)));
            }
            out.add(merged);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> applyUnion(List<Map<String, Object>> rows, Map<String, Object> cfg) {
        List<Map<String, Object>> out = new ArrayList<>(rows);
        Object extra = cfg.get("unionRows");
        if (extra instanceof List<?> extraList) {
            for (Object item : extraList) {
                if (item instanceof Map<?, ?> m) {
                    out.add(new LinkedHashMap<>((Map<String, Object>) m));
                }
            }
        }
        return out;
    }

    private static List<Map<String, Object>> applyPivot(List<Map<String, Object>> rows, Map<String, Object> cfg) {
        String groupField = str(cfg.get("groupBy"));
        String pivotField = str(cfg.get("pivotField"));
        String valueField = str(cfg.get("valueField"));
        if (groupField.isEmpty() || pivotField.isEmpty() || valueField.isEmpty()) {
            return rows;
        }
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String gKey = String.valueOf(row.getOrDefault(groupField, ""));
            String pCol = String.valueOf(row.getOrDefault(pivotField, "Other"));
            double val = toDouble(row.get(valueField));
            Map<String, Object> target = grouped.computeIfAbsent(gKey, k -> {
                Map<String, Object> base = new LinkedHashMap<>();
                base.put(groupField, row.get(groupField));
                return base;
            });
            double prev = toDouble(target.get(pCol));
            target.put(pCol, prev + val);
        }
        return new ArrayList<>(grouped.values());
    }

    private static List<Map<String, Object>> applyAggregate(List<Map<String, Object>> rows, Map<String, Object> cfg) {
        String groupField = str(cfg.getOrDefault("groupBy", cfg.get("field")));
        String measureField = str(cfg.getOrDefault("measureField", "Doanh_So"));
        String aggFunc = str(cfg.getOrDefault("aggregation", "SUM")).toUpperCase(Locale.ROOT);
        if (groupField.isEmpty() || rows.isEmpty()) {
            return rows;
        }
        Map<String, List<Double>> buckets = new LinkedHashMap<>();
        Map<String, Object> rawGroupValues = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object gVal = row.get(groupField);
            String gKey = String.valueOf(gVal);
            rawGroupValues.put(gKey, gVal);
            buckets.computeIfAbsent(gKey, ignored -> new ArrayList<>()).add(toDouble(row.get(measureField)));
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, List<Double>> entry : buckets.entrySet()) {
            List<Double> vals = entry.getValue();
            double sum = vals.stream().mapToDouble(Double::doubleValue).sum();
            Object aggResult = switch (aggFunc) {
                case "COUNT" -> (long) vals.size();
                case "AVG" -> vals.isEmpty() ? 0.0 : sum / vals.size();
                case "MIN" -> vals.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                case "MAX" -> vals.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                default -> (long) sum;
            };
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(groupField, rawGroupValues.get(entry.getKey()));
            row.put(measureField, aggResult);
            out.add(row);
        }
        return out;
    }

    private static List<PrepPreviewResponse.ColumnProfileDto> buildColumnProfiles(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<String> columns = new LinkedHashSet<>();
        for (Map<String, Object> r : rows) {
            columns.addAll(r.keySet());
        }

        long totalRows = rows.size();
        List<PrepPreviewResponse.ColumnProfileDto> profiles = new ArrayList<>();
        for (String col : columns) {
            long nullCount = 0L;
            Set<String> distinct = new LinkedHashSet<>();
            String minVal = null;
            String maxVal = null;
            boolean numeric = false;

            for (Map<String, Object> r : rows) {
                Object val = r.get(col);
                if (val == null || String.valueOf(val).isBlank()) {
                    nullCount++;
                } else {
                    if (val instanceof Number) {
                        numeric = true;
                    }
                    String s = String.valueOf(val);
                    distinct.add(s);
                    if (minVal == null || s.compareTo(minVal) < 0) {
                        minVal = s;
                    }
                    if (maxVal == null || s.compareTo(maxVal) > 0) {
                        maxVal = s;
                    }
                }
            }

            long nonNullCount = totalRows - nullCount;
            long duplicateCount = Math.max(0L, nonNullCount - distinct.size());
            double nullPercent = totalRows == 0
                    ? 0.0
                    : Math.round((nullCount * 1000.0) / totalRows) / 10.0;
            String dataType = numeric ? "Number" : "String";
            String role = DatasourceAndModelService.inferDefaultRole(col, dataType);

            profiles.add(new PrepPreviewResponse.ColumnProfileDto(
                    col,
                    dataType,
                    role,
                    totalRows,
                    nullCount,
                    nullPercent,
                    distinct.size(),
                    duplicateCount,
                    minVal,
                    maxVal
            ));
        }
        return profiles;
    }

    private static int compareNumeric(Object cell, String target) {
        return Double.compare(toDouble(cell), toDouble(target));
    }

    private static double toDouble(Object val) {
        if (val == null) {
            return 0.0;
        }
        if (val instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(val).trim());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private static String str(Object val) {
        return Objects.toString(val, "").trim();
    }

    private record ReplaceNullOutcome(List<Map<String, Object>> rows, long replacedCount) {
    }

    private record ExecutionOutcome(
            long inputRows,
            List<Map<String, Object>> rows,
            long removedDuplicates,
            long replacedNulls,
            List<PrepFlowDto.LineageStepDto> lineage
    ) {
    }
}
