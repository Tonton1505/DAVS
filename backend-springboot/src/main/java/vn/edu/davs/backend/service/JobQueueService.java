package vn.edu.davs.backend.service;

import vn.edu.davs.backend.config.JobProperties;
import vn.edu.davs.backend.enu.JobStatus;
import vn.edu.davs.backend.model.JobRecord;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Enqueues and queries background jobs stored in the PostgreSQL {@code jobs} table.
 */
@Service
@RequiredArgsConstructor
public class JobQueueService {

    static final RowMapper<JobRecord> ROW_MAPPER = JobQueueService::mapRow;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final JobProperties jobProperties;

    /**
     * Enqueues a job for immediate execution with the configured default max attempts.
     */
    public UUID enqueue(String type, Map<String, Object> payload) {
        return enqueue(type, payload, Instant.now(), jobProperties.getMaxAttempts(), null);
    }

    /**
     * Enqueues a job with explicit schedule time, retry limit, and optional creator id.
     */
    public UUID enqueue(
            String type,
            Map<String, Object> payload,
            Instant runAt,
            int maxAttempts,
            UUID createdBy) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Job type must not be blank");
        }
        int effectiveMaxAttempts = maxAttempts > 0 ? maxAttempts : jobProperties.getMaxAttempts();
        Instant effectiveRunAt = runAt != null ? runAt : Instant.now();
        String payloadJson = serializePayload(payload);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("type", type.trim())
                .addValue("payload", payloadJson)
                .addValue("runAt", Timestamp.from(effectiveRunAt))
                .addValue("maxAttempts", effectiveMaxAttempts)
                .addValue("createdBy", createdBy);

        UUID id = jdbcTemplate.queryForObject("""
                INSERT INTO jobs (type, payload, status, run_at, max_attempts, created_by)
                VALUES (:type, CAST(:payload AS jsonb), 'PENDING', :runAt, :maxAttempts, :createdBy)
                RETURNING id
                """, params, UUID.class);
        if (id == null) {
            throw new IllegalStateException("Failed to insert job row");
        }
        return id;
    }

    /**
     * Retrieves a job snapshot by id.
     */
    public Optional<JobRecord> findById(UUID jobId) {
        if (jobId == null) {
            return Optional.empty();
        }
        var rows = jdbcTemplate.query("""
                SELECT id, type, payload::text AS payload_text, status, run_at,
                       attempts, max_attempts, last_error, locked_at, locked_by,
                       created_at, updated_at, created_by
                FROM jobs
                WHERE id = :id
                """, Map.of("id", jobId), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private String serializePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize job payload", ex);
        }
    }

    private static JobRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new JobRecord(
                rs.getObject("id", UUID.class),
                rs.getString("type"),
                rs.getString("payload_text"),
                JobStatus.valueOf(rs.getString("status")),
                toInstant(rs.getTimestamp("run_at")),
                rs.getInt("attempts"),
                rs.getInt("max_attempts"),
                rs.getString("last_error"),
                toInstant(rs.getTimestamp("locked_at")),
                rs.getString("locked_by"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at")),
                rs.getObject("created_by", UUID.class));
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
