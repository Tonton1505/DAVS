package com.davs.notification;

import com.davs.common.job.JobEntity;
import com.davs.common.job.JobHandler;
import com.davs.common.job.JobRepository;
import com.davs.common.job.JobRunner;
import com.davs.common.job.JobStatus;
import com.davs.common.job.JobType;
import com.davs.common.settings.SettingsService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobAndNotificationTests {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private SettingsService settingsService;

    @Mock
    private JavaMailSender mailSender;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(mailSender, "noreply@davs.local");
    }

    @Test
    void notificationService_sendsFormattedEmail() {
        notificationService.send(EmailTemplate.INVITATION, "an@congty.vn", "Nguyễn Văn An");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("an@congty.vn");
        assertThat(sent.getFrom()).isEqualTo("noreply@davs.local");
        assertThat(sent.getSubject()).isEqualTo(EmailTemplate.INVITATION.getDefaultSubject());
        assertThat(sent.getText()).contains("Nguyễn Văn An");
    }

    @Test
    void jobRunner_executesAndMarksSuccess() throws Exception {
        UUID jobId = UUID.randomUUID();
        JobEntity job = new JobEntity(
                jobId, JobType.SEND_EMAIL, "{}", JobStatus.RUNNING,
                Instant.now(), 0, 3, null, Instant.now(), "worker1", Instant.now(), Instant.now(), null
        );

        JobHandler mockHandler = mock(JobHandler.class);
        when(mockHandler.supports()).thenReturn(JobType.SEND_EMAIL);

        JobRunner runner = new JobRunner(jobRepository, settingsService, List.of(mockHandler));
        when(jobRepository.lockNextAvailableJobs(anyString(), anyInt())).thenReturn(List.of(job));

        int executed = runner.pollAndExecuteBatch(10);
        assertThat(executed).isEqualTo(1);

        verify(mockHandler).handle(job);
        verify(jobRepository).markSucceeded(jobId);
    }

    @Test
    void jobRunner_retriesAndFailsAfterMaxAttempts() throws Exception {
        UUID jobId = UUID.randomUUID();
        JobEntity failingJob = new JobEntity(
                jobId, JobType.EXTRACT_REFRESH, "{}", JobStatus.RUNNING,
                Instant.now(), 2, 3, null, Instant.now(), "worker1", Instant.now(), Instant.now(), null
        );

        JobHandler failingHandler = mock(JobHandler.class);
        when(failingHandler.supports()).thenReturn(JobType.EXTRACT_REFRESH);
        doThrow(new RuntimeException("Database timeout")).when(failingHandler).handle(any());

        JobRunner runner = new JobRunner(jobRepository, settingsService, List.of(failingHandler));

        runner.executeSingleJob(failingJob);

        // Attempts was 2, now 3 == maxAttempts -> marks failure
        verify(jobRepository).recordFailureAndReschedule(
                eq(jobId), eq(3), eq(3), eq("Database timeout"), any(Instant.class)
        );
    }
}
