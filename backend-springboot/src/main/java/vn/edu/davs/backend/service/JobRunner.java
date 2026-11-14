package vn.edu.davs.backend.service;

import vn.edu.davs.backend.config.JobProperties;
import vn.edu.davs.backend.model.JobRecord;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Polls and executes background jobs from PostgreSQL using {@code FOR UPDATE SKIP LOCKED}
 * with automatic retry up to {@code max_attempts} (default 3) before marking {@code FAILED}.
 */
@Slf4j
@Component
public class JobRunner {

    private static final int MAX_ERROR_LENGTH = 2000;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate requiresNewTx;
    private final Map<String, JobHandler> handlersByType;
    private final JobProperties jobProperties;
    private final String defaultWorkerId;

    public JobRunner(
            NamedParameterJdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            List<JobHandler> handlers,
            JobProperties jobProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.handlersByType = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(JobHandler::type, Function.identity()));
        this.jobProperties = jobProperties;
        this.defaultWorkerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Scheduled poll loop that drains due jobs when {@code davs.jobs.enabled=true}.
     */
    @Scheduled(fixedDelayString = "${davs.jobs.poll-interval-ms:2000}")
    public void pollScheduled() {
        if (!jobProperties.isEnabled()) {
            return;
        }
        while (pollAndProcessNext(defaultWorkerId)) {
            // Drain all currently due jobs.
        }
    }

    /**
     * Claims and executes at most one due {@code PENDING} job using the default worker id.
     *
     * @return {@code true} if a job was claimed and processed, {@code false} otherwise
     */
    public boolean pollAndProcessNext() {
        return pollAndProcessNext(defaultWorkerId);
    }

    /**
     * Claims and executes at most one due {@code PENDING} job as {@code workerId}.
     *
     * @return {@code true} if a job was claimed and processed, {@code false} otherwise
     */
    public boolean pollAndProcessNext(String workerId) {
        String effectiveWorker = (workerId == null || workerId.isBlank()) ? defaultWorkerId : workerId;
        Optional<JobRecord> claimed = claimNextPendingJob(effectiveWorker);
        if (claimed.isEmpty()) {
            return false;
        }
        executeClaimedJob(claimed.get());
        return true;
    }

    /**
     * Atomically claims the next due {@code PENDING} job using {@code FOR UPDATE SKIP LOCKED}.
     */
    public Optional<JobRecord> claimNextPendingJob(String workerId) {
        return requiresNewTx.execute(status -> {
            List<JobRecord> claimed = jdbcTemplate.query("""
                    WITH next_job AS (
                        SELECT id
                        FROM jobs
                        WHERE status = 'PENDING'
                          AND run_at <= now()
                        ORDER BY run_at ASC, created_at ASC
                        FOR UPDATE SKIP LOCKED
                        LIMIT 1
                    )
                    UPDATE jobs j
                    SET status = 'RUNNING',
                        attempts = j.attempts + 1,
                        locked_at = now(),
                        locked_by = :workerId,
                        updated_at = now()
                    FROM next_job
                    WHERE j.id = next_job.id
                    RETURNING j.id, j.type, j.payload::text AS payload_text, j.status, j.run_at,
                              j.attempts, j.max_attempts, j.last_error,
                              j.locked_at, j.locked_by, j.created_at, j.updated_at, j.created_by
                    """, Map.of("workerId", workerId), JobQueueService.ROW_MAPPER);
            return claimed.stream().findFirst();
        });
    }

    private void executeClaimedJob(JobRecord job) {
        JobHandler handler = handlersByType.get(job.type());
        if (handler == null) {
            handleFailure(job, new IllegalStateException("No JobHandler registered for type: " + job.type()));
            return;
        }
        try {
            handler.handle(job);
            markSucceeded(job.id());
        } catch (Exception ex) {
            handleFailure(job, ex);
        }
    }

    private void markSucceeded(UUID jobId) {
        requiresNewTx.executeWithoutResult(status -> jdbcTemplate.update("""
                UPDATE jobs
                SET status = 'SUCCEEDED',
                    locked_at = NULL,
                    locked_by = NULL,
                    updated_at = now()
                WHERE id = :id
                """, Map.of("id", jobId)));
    }

    private void handleFailure(JobRecord job, Exception ex) {
        String errorMessage = formatError(ex);
        boolean exhausted = job.attempts() >= job.maxAttempts();
        if (exhausted) {
            log.error("Job {} (type={}) failed permanently after {} attempts: {}",
                    job.id(), job.type(), job.attempts(), errorMessage);
            requiresNewTx.executeWithoutResult(status -> jdbcTemplate.update("""
                    UPDATE jobs
                    SET status = 'FAILED',
                        last_error = :lastError,
                        locked_at = NULL,
                        locked_by = NULL,
                        updated_at = now()
                    WHERE id = :id
                    """, new MapSqlParameterSource()
                    .addValue("id", job.id())
                    .addValue("lastError", errorMessage)));
            return;
        }

        long delaySeconds = Math.max(0L, jobProperties.getRetryDelaySeconds()) * job.attempts();
        Instant nextRunAt = Instant.now().plusSeconds(delaySeconds);
        log.warn("Job {} (type={}) attempt {}/{} failed; retrying at {}: {}",
                job.id(), job.type(), job.attempts(), job.maxAttempts(), nextRunAt, errorMessage);
        requiresNewTx.executeWithoutResult(status -> jdbcTemplate.update("""
                UPDATE jobs
                SET status = 'PENDING',
                    last_error = :lastError,
                    locked_at = NULL,
                    locked_by = NULL,
                    run_at = :runAt,
                    updated_at = now()
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", job.id())
                .addValue("lastError", errorMessage)
                .addValue("runAt", Timestamp.from(nextRunAt))));
    }

    private String formatError(Exception ex) {
        String message = ex.getMessage();
        String raw = message == null || message.isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getClass().getSimpleName() + ": " + message;
        return raw.length() > MAX_ERROR_LENGTH ? raw.substring(0, MAX_ERROR_LENGTH) : raw;
    }
}
