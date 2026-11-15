package vn.edu.davs.backend.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.davs.backend.dto.CalculatedFieldDto;
import vn.edu.davs.backend.dto.DashboardActionDto;
import vn.edu.davs.backend.dto.DashboardDto;
import vn.edu.davs.backend.dto.DashboardTileDto;
import vn.edu.davs.backend.dto.ParameterDto;
import vn.edu.davs.backend.dto.SaveCalculatedFieldRequest;
import vn.edu.davs.backend.dto.SaveDashboardRequest;
import vn.edu.davs.backend.dto.SaveParameterRequest;
import vn.edu.davs.backend.dto.SaveSheetRequest;
import vn.edu.davs.backend.dto.SheetDto;
import vn.edu.davs.backend.dto.TooltipConfigDto;
import vn.edu.davs.backend.enu.ChartType;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC persistence for UC15 (Sheets), UC16 (Calculated Fields & Parameters),
 * UC17 (Dashboards), and UC18 (Actions & Tooltips).
 */
@Repository
@RequiredArgsConstructor
public class JdbcVizRepository implements VizRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // 1. Sheets (UC15 & UC18)
    // =========================================================================

    public List<SheetDto> listSheets(UUID workbookId) {
        String sql = """
                SELECT id, workbook_id, name, chart_type, definition, tooltip_config, created_at, updated_at
                FROM sheets
                WHERE workbook_id = ?
                ORDER BY lower(name) ASC
                """;
        return jdbcTemplate.query(sql, this::mapSheetRow, workbookId);
    }

    public Optional<SheetDto> findSheetById(UUID id) {
        String sql = """
                SELECT id, workbook_id, name, chart_type, definition, tooltip_config, created_at, updated_at
                FROM sheets
                WHERE id = ?
                """;
        return jdbcTemplate.query(sql, this::mapSheetRow, id).stream().findFirst();
    }

    public boolean existsSheetName(UUID workbookId, String name, UUID excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM sheets WHERE workbook_id = ? AND lower(name) = lower(?)"
                : "SELECT COUNT(*) FROM sheets WHERE workbook_id = ? AND lower(name) = lower(?) AND id <> ?";
        Integer count = excludeId == null
                ? jdbcTemplate.queryForObject(sql, Integer.class, workbookId, name.trim())
                : jdbcTemplate.queryForObject(sql, Integer.class, workbookId, name.trim(), excludeId);
        return count != null && count > 0;
    }

    public SheetDto createSheet(UUID workbookId, SaveSheetRequest request) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String defJson = toJson(request.definition());
        String tooltipJson = request.tooltipConfig() != null ? toJson(request.tooltipConfig()) : null;

        String sql = """
                INSERT INTO sheets (id, workbook_id, name, chart_type, definition, tooltip_config, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?)
                """;
        jdbcTemplate.update(sql, id, workbookId, request.name().trim(), request.chartType().name(),
                defJson, tooltipJson, now, now);
        return new SheetDto(id, workbookId, request.name().trim(), request.chartType(),
                request.definition(), request.tooltipConfig(), now, now);
    }

    public SheetDto updateSheet(UUID id, SaveSheetRequest request) {
        Instant now = Instant.now();
        String defJson = toJson(request.definition());
        String tooltipJson = request.tooltipConfig() != null ? toJson(request.tooltipConfig()) : null;

        String sql = """
                UPDATE sheets
                SET name = ?, chart_type = ?, definition = ?::jsonb, tooltip_config = ?::jsonb, updated_at = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, request.name().trim(), request.chartType().name(), defJson, tooltipJson, now, id);
        return findSheetById(id).orElse(null);
    }

    public boolean deleteSheet(UUID id) {
        return jdbcTemplate.update("DELETE FROM sheets WHERE id = ?", id) > 0;
    }

    public boolean updateSheetTooltip(UUID sheetId, TooltipConfigDto tooltipConfig) {
        String tooltipJson = tooltipConfig != null ? toJson(tooltipConfig) : null;
        String sql = "UPDATE sheets SET tooltip_config = ?::jsonb, updated_at = ? WHERE id = ?";
        return jdbcTemplate.update(sql, tooltipJson, Instant.now(), sheetId) > 0;
    }

    private SheetDto mapSheetRow(ResultSet rs, int rowNum) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        UUID workbookId = rs.getObject("workbook_id", UUID.class);
        String name = rs.getString("name");
        ChartType chartType = ChartType.valueOf(rs.getString("chart_type"));
        Map<String, Object> definition = fromJson(rs.getString("definition"), new TypeReference<>() {});
        TooltipConfigDto tooltipConfig = fromJson(rs.getString("tooltip_config"), new TypeReference<>() {});
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Instant updatedAt = rs.getTimestamp("updated_at").toInstant();
        return new SheetDto(id, workbookId, name, chartType, definition, tooltipConfig, createdAt, updatedAt);
    }

    // =========================================================================
    // 2. Calculated Fields (UC16)
    // =========================================================================

    public List<CalculatedFieldDto> listCalculatedFields(UUID datasourceId) {
        String sql = """
                SELECT id, datasource_id, name, formula, return_type, role, created_at, updated_at
                FROM calculated_fields
                WHERE datasource_id = ?
                ORDER BY lower(name) ASC
                """;
        return jdbcTemplate.query(sql, this::mapCalcFieldRow, datasourceId);
    }

    public Optional<CalculatedFieldDto> findCalculatedFieldById(UUID id) {
        String sql = """
                SELECT id, datasource_id, name, formula, return_type, role, created_at, updated_at
                FROM calculated_fields
                WHERE id = ?
                """;
        return jdbcTemplate.query(sql, this::mapCalcFieldRow, id).stream().findFirst();
    }

    public boolean existsCalculatedFieldName(UUID datasourceId, String name, UUID excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM calculated_fields WHERE datasource_id = ? AND lower(name) = lower(?)"
                : "SELECT COUNT(*) FROM calculated_fields WHERE datasource_id = ? AND lower(name) = lower(?) AND id <> ?";
        Integer count = excludeId == null
                ? jdbcTemplate.queryForObject(sql, Integer.class, datasourceId, name.trim())
                : jdbcTemplate.queryForObject(sql, Integer.class, datasourceId, name.trim(), excludeId);
        return count != null && count > 0;
    }

    public CalculatedFieldDto createCalculatedField(UUID datasourceId, SaveCalculatedFieldRequest request) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String sql = """
                INSERT INTO calculated_fields (id, datasource_id, name, formula, return_type, role, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, id, datasourceId, request.name().trim(), request.formula().trim(),
                request.returnType(), request.role(), now, now);
        return new CalculatedFieldDto(id, datasourceId, request.name().trim(), request.formula().trim(),
                request.returnType(), request.role(), now, now);
    }

    public CalculatedFieldDto updateCalculatedField(UUID id, SaveCalculatedFieldRequest request) {
        Instant now = Instant.now();
        String sql = """
                UPDATE calculated_fields
                SET name = ?, formula = ?, return_type = ?, role = ?, updated_at = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, request.name().trim(), request.formula().trim(), request.returnType(), request.role(), now, id);
        return findCalculatedFieldById(id).orElse(null);
    }

    public boolean deleteCalculatedField(UUID id) {
        return jdbcTemplate.update("DELETE FROM calculated_fields WHERE id = ?", id) > 0;
    }

    private CalculatedFieldDto mapCalcFieldRow(ResultSet rs, int rowNum) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        UUID datasourceId = rs.getObject("datasource_id", UUID.class);
        String name = rs.getString("name");
        String formula = rs.getString("formula");
        String returnType = rs.getString("return_type");
        String role = rs.getString("role");
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Instant updatedAt = rs.getTimestamp("updated_at").toInstant();
        return new CalculatedFieldDto(id, datasourceId, name, formula, returnType, role, createdAt, updatedAt);
    }

    // =========================================================================
    // 3. Parameters (UC16)
    // =========================================================================

    public List<ParameterDto> listParameters(UUID workbookId) {
        String sql = """
                SELECT id, workbook_id, name, data_type, allowable_values_type, list_values,
                       range_min, range_max, range_step, current_value, created_at, updated_at
                FROM parameters
                WHERE workbook_id = ?
                ORDER BY lower(name) ASC
                """;
        return jdbcTemplate.query(sql, this::mapParameterRow, workbookId);
    }

    public Optional<ParameterDto> findParameterById(UUID id) {
        String sql = """
                SELECT id, workbook_id, name, data_type, allowable_values_type, list_values,
                       range_min, range_max, range_step, current_value, created_at, updated_at
                FROM parameters
                WHERE id = ?
                """;
        return jdbcTemplate.query(sql, this::mapParameterRow, id).stream().findFirst();
    }

    public boolean existsParameterName(UUID workbookId, String name, UUID excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM parameters WHERE workbook_id = ? AND lower(name) = lower(?)"
                : "SELECT COUNT(*) FROM parameters WHERE workbook_id = ? AND lower(name) = lower(?) AND id <> ?";
        Integer count = excludeId == null
                ? jdbcTemplate.queryForObject(sql, Integer.class, workbookId, name.trim())
                : jdbcTemplate.queryForObject(sql, Integer.class, workbookId, name.trim(), excludeId);
        return count != null && count > 0;
    }

    public ParameterDto createParameter(UUID workbookId, SaveParameterRequest request) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String listJson = request.listValues() != null ? toJson(request.listValues()) : null;

        String sql = """
                INSERT INTO parameters (id, workbook_id, name, data_type, allowable_values_type,
                                        list_values, range_min, range_max, range_step, current_value,
                                        created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, id, workbookId, request.name().trim(), request.dataType(), request.allowableValuesType(),
                listJson, request.rangeMin(), request.rangeMax(), request.rangeStep(), request.currentValue(), now, now);
        return new ParameterDto(id, workbookId, request.name().trim(), request.dataType(), request.allowableValuesType(),
                request.listValues(), request.rangeMin(), request.rangeMax(), request.rangeStep(), request.currentValue(), now, now);
    }

    public ParameterDto updateParameter(UUID id, SaveParameterRequest request) {
        Instant now = Instant.now();
        String listJson = request.listValues() != null ? toJson(request.listValues()) : null;

        String sql = """
                UPDATE parameters
                SET name = ?, data_type = ?, allowable_values_type = ?, list_values = ?::jsonb,
                    range_min = ?, range_max = ?, range_step = ?, current_value = ?, updated_at = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, request.name().trim(), request.dataType(), request.allowableValuesType(),
                listJson, request.rangeMin(), request.rangeMax(), request.rangeStep(), request.currentValue(), now, id);
        return findParameterById(id).orElse(null);
    }

    public boolean deleteParameter(UUID id) {
        return jdbcTemplate.update("DELETE FROM parameters WHERE id = ?", id) > 0;
    }

    private ParameterDto mapParameterRow(ResultSet rs, int rowNum) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        UUID workbookId = rs.getObject("workbook_id", UUID.class);
        String name = rs.getString("name");
        String dataType = rs.getString("data_type");
        String allowableValuesType = rs.getString("allowable_values_type");
        List<String> listValues = fromJson(rs.getString("list_values"), new TypeReference<>() {});
        BigDecimal rangeMin = rs.getBigDecimal("range_min");
        BigDecimal rangeMax = rs.getBigDecimal("range_max");
        BigDecimal rangeStep = rs.getBigDecimal("range_step");
        String currentValue = rs.getString("current_value");
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Instant updatedAt = rs.getTimestamp("updated_at").toInstant();
        return new ParameterDto(id, workbookId, name, dataType, allowableValuesType, listValues,
                rangeMin, rangeMax, rangeStep, currentValue, createdAt, updatedAt);
    }

    // =========================================================================
    // 4. Dashboards (UC17 & UC18)
    // =========================================================================

    public List<DashboardDto> listDashboards(UUID workbookId) {
        String sql = """
                SELECT id, workbook_id, name, description, size_type, width, height, tiles, actions, created_at, updated_at
                FROM dashboards
                WHERE workbook_id = ?
                ORDER BY lower(name) ASC
                """;
        return jdbcTemplate.query(sql, this::mapDashboardRow, workbookId);
    }

    public Optional<DashboardDto> findDashboardById(UUID id) {
        String sql = """
                SELECT id, workbook_id, name, description, size_type, width, height, tiles, actions, created_at, updated_at
                FROM dashboards
                WHERE id = ?
                """;
        return jdbcTemplate.query(sql, this::mapDashboardRow, id).stream().findFirst();
    }

    public boolean existsDashboardName(UUID workbookId, String name, UUID excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM dashboards WHERE workbook_id = ? AND lower(name) = lower(?)"
                : "SELECT COUNT(*) FROM dashboards WHERE workbook_id = ? AND lower(name) = lower(?) AND id <> ?";
        Integer count = excludeId == null
                ? jdbcTemplate.queryForObject(sql, Integer.class, workbookId, name.trim())
                : jdbcTemplate.queryForObject(sql, Integer.class, workbookId, name.trim(), excludeId);
        return count != null && count > 0;
    }

    public DashboardDto createDashboard(UUID workbookId, SaveDashboardRequest request) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String tilesJson = toJson(request.tiles());
        String actionsJson = toJson(request.actions());

        String sql = """
                INSERT INTO dashboards (id, workbook_id, name, description, size_type, width,
                                        height, tiles, actions, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?)
                """;
        jdbcTemplate.update(sql, id, workbookId, request.name().trim(), request.description(), request.sizeType(),
                request.width(), request.height(), tilesJson, actionsJson, now, now);
        return new DashboardDto(id, workbookId, request.name().trim(), request.description(), request.sizeType(),
                request.width(), request.height(), request.tiles(), request.actions(), now, now);
    }

    public DashboardDto updateDashboard(UUID id, SaveDashboardRequest request) {
        Instant now = Instant.now();
        String tilesJson = toJson(request.tiles());
        String actionsJson = toJson(request.actions());

        String sql = """
                UPDATE dashboards
                SET name = ?, description = ?, size_type = ?, width = ?, height = ?,
                    tiles = ?::jsonb, actions = ?::jsonb, updated_at = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, request.name().trim(), request.description(), request.sizeType(),
                request.width(), request.height(), tilesJson, actionsJson, now, id);
        return findDashboardById(id).orElse(null);
    }

    public boolean deleteDashboard(UUID id) {
        return jdbcTemplate.update("DELETE FROM dashboards WHERE id = ?", id) > 0;
    }

    public boolean updateDashboardActions(UUID dashboardId, List<DashboardActionDto> actions) {
        String actionsJson = toJson(actions);
        String sql = "UPDATE dashboards SET actions = ?::jsonb, updated_at = ? WHERE id = ?";
        return jdbcTemplate.update(sql, actionsJson, Instant.now(), dashboardId) > 0;
    }

    private DashboardDto mapDashboardRow(ResultSet rs, int rowNum) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        UUID workbookId = rs.getObject("workbook_id", UUID.class);
        String name = rs.getString("name");
        String description = rs.getString("description");
        String sizeType = rs.getString("size_type");
        Integer width = rs.getInt("width");
        Integer height = rs.getInt("height");
        List<DashboardTileDto> tiles = fromJson(rs.getString("tiles"), new TypeReference<>() {});
        List<DashboardActionDto> actions = fromJson(rs.getString("actions"), new TypeReference<>() {});
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Instant updatedAt = rs.getTimestamp("updated_at").toInstant();
        return new DashboardDto(id, workbookId, name, description, sizeType, width, height,
                tiles, actions, createdAt, updatedAt);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> ref) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ref);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
