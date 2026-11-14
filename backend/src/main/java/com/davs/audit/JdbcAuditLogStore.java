package com.davs.audit;

import com.davs.common.api.PageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC-backed {@link AuditLogStore} interacting directly with PostgreSQL {@code audit_logs}.
 */
@Repository
@RequiredArgsConstructor
public class JdbcAuditLogStore implements AuditLogStore {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void insert(
            String action,
            UUID actorId,
            String actorEmail,
            String resourceType,
            String resourceId,
            JsonNode beforeState,
            JsonNode afterState,
            String ipAddress,
            String userAgent,
            String traceId
    ) {
        String sql = "INSERT INTO audit_logs "
                + "(action, actor_id, actor_email, resource_type, resource_id, "
                + "before_state, after_state, ip_address, user_agent, trace_id) "
                + "VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?)";

        jdbcTemplate.update(
                sql,
                action,
                actorId,
                actorEmail,
                resourceType,
                resourceId,
                toJson(beforeState),
                toJson(afterState),
                ipAddress,
                userAgent,
                traceId
        );
    }

    @Override
    public PageResponse<AuditLogResponse> search(AuditLogFilter filter, Pageable pageable) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (filter != null) {
            if (filter.actorId() != null) {
                where.append(" AND actor_id = ?");
                params.add(filter.actorId());
            }
            if (filter.actorEmail() != null && !filter.actorEmail().isBlank()) {
                where.append(" AND lower(actor_email) = lower(?)");
                params.add(filter.actorEmail().trim());
            }
            if (filter.action() != null && !filter.action().isBlank()) {
                where.append(" AND action = ?");
                params.add(filter.action().trim());
            }
            if (filter.resourceType() != null && !filter.resourceType().isBlank()) {
                where.append(" AND resource_type = ?");
                params.add(filter.resourceType().trim());
            }
            if (filter.from() != null) {
                where.append(" AND created_at >= ?");
                params.add(Timestamp.from(filter.from()));
            }
            if (filter.to() != null) {
                where.append(" AND created_at <= ?");
                params.add(Timestamp.from(filter.to()));
            }
        }

        String countSql = "SELECT COUNT(*) FROM audit_logs" + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        long totalElements = total != null ? total : 0L;

        String orderBy = resolveOrderBy(pageable);
        String selectSql = "SELECT id, action, actor_id, actor_email, resource_type, resource_id, "
                + "before_state, after_state, ip_address, user_agent, trace_id, created_at "
                + "FROM audit_logs"
                + where
                + " ORDER BY " + orderBy
                + " LIMIT ? OFFSET ?";

        List<Object> selectParams = new ArrayList<>(params);
        selectParams.add(pageable.getPageSize());
        selectParams.add(pageable.getOffset());

        List<AuditLogResponse> content = jdbcTemplate.query(
                selectSql,
                (rs, rowNum) -> new AuditLogResponse(
                        rs.getObject("id", UUID.class),
                        rs.getString("action"),
                        rs.getObject("actor_id", UUID.class),
                        rs.getString("actor_email"),
                        rs.getString("resource_type"),
                        rs.getString("resource_id"),
                        fromJson(rs.getString("before_state")),
                        fromJson(rs.getString("after_state")),
                        rs.getString("ip_address"),
                        rs.getString("user_agent"),
                        rs.getString("trace_id"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                selectParams.toArray()
        );

        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), totalElements);
    }

    private String resolveOrderBy(Pageable pageable) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return "created_at DESC";
        }
        Sort.Order order = pageable.getSort().iterator().next();
        String column = switch (order.getProperty()) {
            case "action" -> "action";
            case "actorEmail" -> "actor_email";
            case "createdAt" -> "created_at";
            default -> "created_at";
        };
        String direction = order.isAscending() ? "ASC" : "DESC";
        return column + " " + direction;
    }

    private String toJson(JsonNode node) {
        if (node == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize audit state JSON", ex);
        }
    }

    private JsonNode fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse audit state JSON", ex);
        }
    }
}
