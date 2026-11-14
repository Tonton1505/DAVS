package com.davs.notification;

import com.davs.job.JobQueueService;
import com.davs.job.JobRecord;
import com.davs.job.JobRunner;
import com.davs.job.JobStatus;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "davs.jobs.enabled=false"
})
class NotificationMailHogIntegrationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("davs")
            .withUsername("davs")
            .withPassword("davs_test_pwd");

    @Container
    static final GenericContainer<?> MAILHOG = new GenericContainer<>("mailhog/mailhog:v1.0.1")
            .withExposedPorts(1025, 8025);

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", MAILHOG::getHost);
        registry.add("spring.mail.port", () -> MAILHOG.getMappedPort(1025));
    }

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JobRunner jobRunner;

    @Autowired
    private JobQueueService jobQueueService;

    @Test
    void sendsSyncAndAsyncTemplatesToMailHog() throws Exception {
        notificationService.send(
                EmailTemplate.INVITE,
                "invitee@davs.local",
                Map.of(
                        "inviterName", "Admin",
                        "role", "Creator",
                        "inviteUrl", "http://localhost:3000/accept-invite?token=abc"));

        UUID jobId = notificationService.sendAsync(
                EmailTemplate.MENTION,
                "analyst@davs.local",
                Map.of(
                        "authorName", "Le Thu Ha",
                        "targetName", "Sales Dashboard",
                        "commentExcerpt", "@analyst vui long kiem tra doanh thu Q4",
                        "commentUrl", "http://localhost:3000/workbooks/1"));

        assertThat(jobRunner.pollAndProcessNext("mailhog-worker")).isTrue();
        JobRecord completed = jobQueueService.findById(jobId).orElseThrow();
        assertThat(completed.status()).isEqualTo(JobStatus.SUCCEEDED);

        String apiUrl = "http://" + MAILHOG.getHost() + ":" + MAILHOG.getMappedPort(8025) + "/api/v2/messages";
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(apiUrl)).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        String normalizedBody = response.body().replace("=\\r\\n", "").replace("=\\n", "");
        assertThat(normalizedBody).contains("\"total\":2");
        assertThat(normalizedBody).contains("invitee@davs.local");
        assertThat(normalizedBody).contains("analyst@davs.local");
        assertThat(normalizedBody).contains("Sales Dashboard");
    }
}
