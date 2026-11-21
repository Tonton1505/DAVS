package vn.edu.davs.backend.dao.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.davs.backend.dto.CustomViewDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.edu.davs.backend.dao.ICustomViewDao;

@Slf4j
@RequiredArgsConstructor
@Repository
public class CustomViewDaoImpl implements ICustomViewDao {


    private final JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public Optional<CustomViewDto> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT cv.id, cv.dashboard_id, cv.name, cv.filters, cv.parameters, cv.is_default, cv.is_shared,
                       cv.owner_id, u.full_name AS owner_name, cv.created_at, cv.updated_at
                FROM custom_views cv
                JOIN users u ON u.id = cv.owner_id
                WHERE cv.id = ?
                """;
        List<CustomViewDto> list = jdbcTemplate.query(sql, this::mapRow, id);
        return list.stream().findFirst();
    }

    @Override
    public List<CustomViewDto> findVisibleForUser(UUID dashboardId, UUID userId) {
        // BUS-VIZ-18: Custom View only visible to its author unless is_shared = true
        String sql = """
                SELECT cv.id, cv.dashboard_id, cv.name, cv.filters, cv.parameters, cv.is_default, cv.is_shared,
                       cv.owner_id, u.full_name AS owner_name, cv.created_at, cv.updated_at
                FROM custom_views cv
                JOIN users u ON u.id = cv.owner_id
                WHERE cv.dashboard_id = ? AND (cv.owner_id = ? OR cv.is_shared = TRUE)
                ORDER BY cv.is_default DESC, cv.created_at DESC
                """;
        return jdbcTemplate.query(sql, this::mapRow, dashboardId, userId);
    }

    @Override
    public CustomViewDto create(UUID dashboardId, String name, String filtersJson, String paramsJson,
                                 boolean isDefault, boolean isShared, UUID ownerId, String ownerName) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        if (isDefault) {
            unsetDefault(dashboardId, ownerId);
        }
        String sql = """
                INSERT INTO custom_views (
                    id, dashboard_id, name, filters, parameters, is_default, is_shared, owner_id, created_at, updated_at
                ) VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, id, dashboardId, name, filtersJson, paramsJson, isDefault, isShared, ownerId, now, now);
        return new CustomViewDto(
                id, dashboardId, name, parseMap(filtersJson), parseMap(paramsJson),
                isDefault, isShared, ownerId, ownerName, now, now
        );
    }

    @Override
    public void setDefault(UUID id, UUID dashboardId, UUID ownerId) {
        unsetDefault(dashboardId, ownerId);
        String sql = "UPDATE custom_views SET is_default = TRUE, updated_at = now() WHERE id = ? AND owner_id = ?";
        jdbcTemplate.update(sql, id, ownerId);
    }

    @Override
    public void unsetDefault(UUID dashboardId, UUID ownerId) {
        String sql = "UPDATE custom_views SET is_default = FALSE, updated_at = now() WHERE dashboard_id = ? AND owner_id = ?";
        jdbcTemplate.update(sql, dashboardId, ownerId);
    }

    @Override
    public void setShared(UUID id, UUID ownerId, boolean shared) {
        String sql = "UPDATE custom_views SET is_shared = ?, updated_at = now() WHERE id = ? AND owner_id = ?";
        jdbcTemplate.update(sql, shared, id, ownerId);
    }

    @Override
    public boolean delete(UUID id, UUID ownerId) {
        String sql = "DELETE FROM custom_views WHERE id = ? AND owner_id = ?";
        return jdbcTemplate.update(sql, id, ownerId) > 0;
    }


    private CustomViewDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        String filtersJson = rs.getString("filters");
        String paramsJson = rs.getString("parameters");
        return new CustomViewDto(
                rs.getObject("id", UUID.class),
                rs.getObject("dashboard_id", UUID.class),
                rs.getString("name"),
                parseMap(filtersJson),
                parseMap(paramsJson),
                rs.getBoolean("is_default"),
                rs.getBoolean("is_shared"),
                rs.getObject("owner_id", UUID.class),
                rs.getString("owner_name"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }


    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse json map: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
