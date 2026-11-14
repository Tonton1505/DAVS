package com.davs.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "davs.jobs.enabled=false",
        "davs.jobs.retry-delay-seconds=0",
        "davs.jobs.max-attempts=3"
})
class JobRunnerPostgresIntegrationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("davs")
            .withUsername("davs")
            .withPassword("davs_test_pwd");

    private static final ConcurrentHashMap<UUID, AtomicInteger> EXECUTION_COUNTS = new ConcurrentHashMap<>();

    @TestConfiguration
    static class TestHandlersConfig {

        @Bean
        JobHandler recordingSuccessHandler() {
            return new JobHandler() {
                @Override
                public String type() {
                    return "TEST_RECORD";
                }

                @Override
                public void handle(JobRecord job) throws Exception {
                    EXECUTION_COUNTS.computeIfAbsent(job.id(), ignored -> new AtomicInteger()).incrementAndGet();
                    Thread.sleep(25);
                }
            };
        }

        @Bean
        JobHandler alwaysFailingHandler() {
            return new JobHandler() {
                @Override
                public String type() {
                    return "TEST_FAIL";
                }

                @Override
                public void handle(JobRecord job) {
                    EXECUTION_COUNTS.computeIfAbsent(job.id(), ignored -> new AtomicInteger()).incrementAndGet();
                    throw new IllegalStateException("simulated failure");
                }
            };
        }
    }

    @Autowired
    private JobQueueService jobQueueService;

    @Autowired
    private JobRunner jobRunner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanJobs() {
        EXECUTION_COUNTS.clear();
        jdbcTemplate.update("DELETE FROM jobs");
    }

    @Test
    void twoParallelRunnersNeverProcessTheSameJobTwice() throws Exception {
        int jobCount = 12;
        List<UUID> jobIds = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            jobIds.add(jobQueueService.enqueue("TEST_RECORD", Map.of("index", i)));
        }

        CountDownLatch startGate = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Integer> worker1 = pool.submit(() -> drainQueue("runner-1", startGate));
            Future<Integer> worker2 = pool.submit(() -> drainQueue("runner-2", startGate));
            startGate.countDown();

            int processed1 = worker1.get(15, TimeUnit.SECONDS);
            int processed2 = worker2.get(15, TimeUnit.SECONDS);
            assertThat(processed1 + processed2).isEqualTo(jobCount);
        }

        for (UUID jobId : jobIds) {
            assertThat(EXECUTION_COUNTS.get(jobId).get()).isEqualTo(1);
            JobRecord record = jobQueueService.findById(jobId).orElseThrow();
            assertThat(record.status()).isEqualTo(JobStatus.SUCCEEDED);
            assertThat(record.attempts()).isEqualTo(1);
        }
    }

    @Test
    void failingJobRetriesThreeTimesThenBecomesFailed() {
        UUID jobId = jobQueueService.enqueue("TEST_FAIL", Map.of("reason", "test"));

        assertThat(jobRunner.pollAndProcessNext("retry-worker")).isTrue();
        JobRecord after1 = jobQueueService.findById(jobId).orElseThrow();
        assertThat(after1.status()).isEqualTo(JobStatus.PENDING);
        assertThat(after1.attempts()).isEqualTo(1);
        assertThat(after1.lastError()).contains("simulated failure");

        assertThat(jobRunner.pollAndProcessNext("retry-worker")).isTrue();
        JobRecord after2 = jobQueueService.findById(jobId).orElseThrow();
        assertThat(after2.status()).isEqualTo(JobStatus.PENDING);
        assertThat(after2.attempts()).isEqualTo(2);

        assertThat(jobRunner.pollAndProcessNext("retry-worker")).isTrue();
        JobRecord after3 = jobQueueService.findById(jobId).orElseThrow();
        assertThat(after3.status()).isEqualTo(JobStatus.FAILED);
        assertThat(after3.attempts()).isEqualTo(3);
        assertThat(after3.lastError()).contains("simulated failure");

        assertThat(jobRunner.pollAndProcessNext("retry-worker")).isFalse();
        assertThat(EXECUTION_COUNTS.get(jobId).get()).isEqualTo(3);
    }

    private int drainQueue(String workerId, CountDownLatch startGate) throws InterruptedException {
        startGate.await();
        int count = 0;
        while (jobRunner.pollAndProcessNext(workerId)) {
            count++;
        }
        return count;
    }
}
