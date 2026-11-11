package com.davs.common.job;

import com.davs.common.settings.SettingsService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobRunner {

    private static final Logger log = LoggerFactory.getLogger(JobRunner.class);
    private final JobRepository jobRepository;
    private final SettingsService settingsService;
    private final Map<JobType, JobHandler> handlers;
    private final String runnerId = "runner-" + UUID.randomUUID().toString().substring(0, 8);

    public JobRunner(
            JobRepository jobRepository,
            SettingsService settingsService,
            List<JobHandler> handlerList
    ) {
        this.jobRepository = jobRepository;
        this.settingsService = settingsService;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(JobHandler::supports, Function.identity()));
    }

    public String getRunnerId() {
        return runnerId;
    }

    @Transactional
    public int pollAndExecuteBatch(int batchSize) {
        List<JobEntity> lockedJobs = jobRepository.lockNextAvailableJobs(runnerId, batchSize);
        int executed = 0;

        for (JobEntity job : lockedJobs) {
            if (isConcurrencyExceeded(job.type())) {
                jobRepository.recordFailureAndReschedule(
                        job.id(),
                        job.attempts(),
                        job.maxAttempts(),
                        "Concurrency limit reached, deferred",
                        Instant.now().plus(Duration.ofSeconds(10))
                );
                continue;
            }

            executeSingleJob(job);
            executed++;
        }
        return executed;
    }

    private boolean isConcurrencyExceeded(JobType type) {
        if (type == JobType.EXTRACT_REFRESH) {
            int maxConcurrent = settingsService.getInt(SettingsService.KEY_REFRESH_MAX_CONCURRENT, 4);
            int running = jobRepository.countRunningJobsByType(type);
            return running > maxConcurrent;
        }
        return false;
    }

    public void executeSingleJob(JobEntity job) {
        JobHandler handler = handlers.get(job.type());
        if (handler == null) {
            jobRepository.recordFailureAndReschedule(
                    job.id(),
                    job.maxAttempts(),
                    job.maxAttempts(),
                    "No handler registered for job type " + job.type(),
                    Instant.now()
            );
            return;
        }

        try {
            handler.handle(job);
            jobRepository.markSucceeded(job.id());
        } catch (Exception ex) {
            int nextAttempts = job.attempts() + 1;
            int backoffSec = (int) Math.pow(2, nextAttempts) * 5;
            Instant nextRunAt = Instant.now().plus(Duration.ofSeconds(backoffSec));
            String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();

            jobRepository.recordFailureAndReschedule(
                    job.id(),
                    nextAttempts,
                    job.maxAttempts(),
                    errorMsg,
                    nextRunAt
            );
        }
    }
}
