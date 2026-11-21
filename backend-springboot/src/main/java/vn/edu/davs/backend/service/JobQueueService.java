package vn.edu.davs.backend.service;

import vn.edu.davs.backend.model.JobRecord;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Enqueues and queries background jobs stored in the PostgreSQL {@code jobs} table.
 */
public interface JobQueueService {


    /**
     * Enqueues a job for immediate execution with the configured default max attempts.
     */
    UUID enqueue(String type, Map<String, Object> payload);


    /**
     * Enqueues a job with explicit schedule time, retry limit, and optional creator id.
     */
    UUID enqueue(
            String type,
            Map<String, Object> payload,
            Instant runAt,
            int maxAttempts,
            UUID createdBy);


    /**
     * Retrieves a job snapshot by id.
     */
    Optional<JobRecord> findById(UUID jobId);
}
