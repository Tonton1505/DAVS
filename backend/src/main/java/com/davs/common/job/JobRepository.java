package com.davs.common.job;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JobRepository {

    private final JdbcTemplate jdbcTemplate;

    public JobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<JobEntity> MAPPER = (rs, rowNum) -> new JobEntity(
            UUID.fromString(rs.getString("id")),
            JobType.valueOf(rs.getString("type")),
            rs.getString("payload"),
            JobStatus.valueOf(rs.getString("status")),
            toInstant(rs.getTimestamp("run_at")),
            rs.getInt("attempts"),
            rs.getInt("max_attempts"),
            rs.getString("last_error"),
            toInstant(rs.getTimestamp("locked_at")),
            rs.getString("locked_by"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")),
            rs.getString("created_by") != null ? UUID.fromString(rs.getString("created_by")) : null
    );

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }

    public UUID enqueue(JobType type, String payload, Instant runAt) {
        UUID id = UUID.randomUUID();
        Instant run = runAt != null ? runAt : Instant.now();
        String jsonPayload = (payload == null || payload.isBlank()) ? "{}" : payload;

        jdbcTemplate.update(
                "INSERT INTO jobs (id, type, payload, status, run_at, attempts, max_attempts, created_at, updated_at) "
                        + "VALUES (?, ?, ?::jsonb, 'PENDING', ?, 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id, type.name(), jsonPayload, Timestamp.from(run)
        );
        return id;
    }

    public List<JobEntity> lockNextAvailableJobs(String workerId, int limit) {
        String sql = """
                UPDATE jobs
                SET status = 'RUNNING',
                    locked_at = CURRENT_TIMESTAMP,
                    locked_by = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id IN (
                    SELECT id FROM jobs
                    WHERE status = 'PENDING' AND run_at <= CURRENT_TIMESTAMP
                    ORDER BY run_at ASC
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                )
                RETURNING id, type, payload::text as payload, status, run_at, attempts, max_attempts,
                          last_error, locked_at, locked_by, created_at, updated_at, created_by
                """;
        return jdbcTemplate.query(sql, MAPPER, workerId, limit);
    }

    public void markSucceeded(UUID jobId) {
        jdbcTemplate.update(
                "UPDATE jobs SET status = 'SUCCEEDED', updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                jobId
        );
    }

    public void recordFailureAndReschedule(UUID jobId, int nextAttempts, int maxAttempts, String errorMsg, Instant nextRunAt) {
        JobStatus nextStatus = nextAttempts >= maxAttempts ? JobStatus.FAILED : JobStatus.PENDING;
        jdbcTemplate.update(
                "UPDATE jobs SET status = ?, attempts = ?, last_error = ?, run_at = ?, "
                        + "locked_at = NULL, locked_by = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                nextStatus.name(),
                nextAttempts,
                errorMsg,
                Timestamp.from(nextRunAt),
                jobId
        );
    }

    public int countRunningJobsByType(JobType type) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM jobs WHERE type = ? AND status = 'RUNNING'",
                Integer.class,
                type.name()
        );
        return count != null ? count : 0;
    }

    public Optional<JobEntity> findById(UUID jobId) {
        try {
            JobEntity job = jdbcTemplate.queryForObject(
                    "SELECT id, type, payload::text as payload, status, run_at, attempts, max_attempts, "
                            + "last_error, locked_at, locked_by, created_at, updated_at, created_by FROM jobs WHERE id = ?",
                    MAPPER,
                    jobId
            );
            return Optional.ofNullable(job);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
