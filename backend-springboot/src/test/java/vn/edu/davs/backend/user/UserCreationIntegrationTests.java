package vn.edu.davs.backend.user;

import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.security.JwtService;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.service.JobRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserCreationIntegrationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("davs")
            .withUsername("davs")
            .withPassword("davs_test_pwd");

    private static volatile boolean failMailSend = false;

    @TestConfiguration
    static class StubMailConfig {
        @Bean
        @Primary
        JavaMailSender stubJavaMailSender() {
            return new JavaMailSender() {
                @Override
                public MimeMessage createMimeMessage() {
                    return new MimeMessage(Session.getInstance(new Properties()));
                }

                @Override
                public MimeMessage createMimeMessage(InputStream contentStream) {
                    return createMimeMessage();
                }

                @Override
                public void send(MimeMessage mimeMessage) {
                    if (failMailSend) {
                        throw new MailSendException("SMTP unavailable");
                    }
                }

                @Override
                public void send(MimeMessage... mimeMessages) {
                    if (failMailSend) {
                        throw new MailSendException("SMTP unavailable");
                    }
                }

                @Override
                public void send(SimpleMailMessage simpleMessage) {
                    if (failMailSend) {
                        throw new MailSendException("SMTP unavailable");
                    }
                }

                @Override
                public void send(SimpleMailMessage... simpleMessages) {
                    if (failMailSend) {
                        throw new MailSendException("SMTP unavailable");
                    }
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JobRunner jobRunner;

    private String adminToken;
    private String creatorToken;

    @BeforeEach
    void setUp() {
        failMailSend = false;
        jdbcTemplate.update("UPDATE license_pools SET used = 0, total = 5 WHERE role = 'SITE_ADMIN'");
        jdbcTemplate.update("UPDATE license_pools SET used = 0, total = 10 WHERE role = 'CREATOR'");
        jdbcTemplate.update("UPDATE license_pools SET used = 0, total = 30 WHERE role = 'EXPLORER'");
        jdbcTemplate.update("UPDATE license_pools SET used = 0, total = 200 WHERE role = 'VIEWER'");

        UUID adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE site_role = 'SITE_ADMIN' AND deleted_at IS NULL LIMIT 1",
                UUID.class
        );
        adminToken = jwtService.generateAccessToken(
                DavsUser.of(adminId, "admin@davs.local", "Admin", SiteRole.SITE_ADMIN)
        );
        creatorToken = jwtService.generateAccessToken(
                DavsUser.of(UUID.randomUUID(), "creator@davs.local", "Creator", SiteRole.CREATOR)
        );
    }

    @Test
    void UC02_nonAdminIsForbiddenFromUserProvisioning() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authMethod":"LOCAL","usernames":["new.user@davs.local"],"role":"VIEWER"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR-PRJ-03"));
    }

    @Test
    void UC02_createMultipleUsersAndHandleDuplicatesAndInvalidEmail() throws Exception {
        String email1 = "u1-" + UUID.randomUUID() + "@davs.local";
        String email2 = "u2-" + UUID.randomUUID() + "@davs.local";

        // [6a] Invalid email returns 400 ERR-USR-01
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authMethod":"LOCAL","usernames":["not-an-email"],"role":"EXPLORER"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR-USR-01"));

        // Create two users
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authMethod":"SSO","usernames":["%s","%s"],"role":"EXPLORER"}
                                """.formatted(email1, email2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.skippedCount").value(0))
                .andExpect(jsonPath("$.created[0].status").value("PENDING"));

        // [6b] Submitting only existing user returns 409 ERR-USR-02
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authMethod":"LOCAL","usernames":["%s"],"role":"EXPLORER"}
                                """.formatted(email1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ERR-USR-02"));

        // [6b] Batch with 1 duplicate + 1 new skips duplicate and creates the new one
        String email3 = "u3-" + UUID.randomUUID() + "@davs.local";
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authMethod":"LOCAL","usernames":["%s","%s"],"role":"EXPLORER"}
                                """.formatted(email1, email3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.skipped[0].code").value("ERR-USR-02"));

        Integer usedExplorer = jdbcTemplate.queryForObject(
                "SELECT used FROM license_pools WHERE role = 'EXPLORER'",
                Integer.class
        );
        assertThat(usedExplorer).isEqualTo(3);
    }

    @Test
    void UC02_exceedingLicenseReturnsErrUsr04AndMailFailureMarksUninvited() throws Exception {
        // [6c] Set SITE_ADMIN pool total = 1, used = 1
        jdbcTemplate.update("UPDATE license_pools SET total = 1, used = 1 WHERE role = 'SITE_ADMIN'");
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authMethod":"LOCAL","usernames":["extra.admin@davs.local"],"role":"SITE_ADMIN"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ERR-USR-04"));

        // [7a] Mail failure marks user UNINVITED
        String uninvitedEmail = "mailfail-" + UUID.randomUUID() + "@davs.local";
        failMailSend = true;

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authMethod":"LOCAL","usernames":["%s"],"role":"VIEWER"}
                                """.formatted(uninvitedEmail)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.uninvitedCount").value(1))
                .andExpect(jsonPath("$.created[0].status").value("UNINVITED"));
    }

    @Test
    void UC02_csvImportProcessesValidAndErrorRowsAndRejectsOver1000Rows() throws Exception {
        mockMvc.perform(get("/api/v1/users/import-template")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("email,full_name,role")));

        // BUS-USR-03: 1001 rows rejected with ERR-USR-03
        StringBuilder tooMany = new StringBuilder("email,full_name,role\n");
        for (int i = 1; i <= 1001; i++) {
            tooMany.append("bulk").append(i).append("@davs.local,User ").append(i).append(",VIEWER\n");
        }
        MockMultipartFile tooManyFile = new MockMultipartFile(
                "file", "too-many.csv", "text/csv", tooMany.toString().getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/users/import")
                        .file(tooManyFile)
                        .param("authMethod", "LOCAL")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ERR-USR-03"));

        // [3a] + [3b] Valid row + invalid email row
        String validCsvEmail = "csv-" + UUID.randomUUID() + "@davs.local";
        String mixedCsv = "email,full_name,role\n"
                + validCsvEmail + ",CSV User,CREATOR\n"
                + "bad-email,Bad User,CREATOR\n";
        MockMultipartFile mixedFile = new MockMultipartFile(
                "file", "users.csv", "text/csv", mixedCsv.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult enqueueResult = mockMvc.perform(multipart("/api/v1/users/import")
                        .file(mixedFile)
                        .param("authMethod", "LOCAL")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isNotEmpty())
                .andReturn();

        String jobId = objectMapper.readTree(enqueueResult.getResponse().getContentAsString())
                .path("jobId").asText();

        jobRunner.pollScheduled();

        mockMvc.perform(get("/api/v1/users/import/" + jobId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.errorCount").value(1))
                .andExpect(jsonPath("$.hasErrorReport").value(true));

        mockMvc.perform(get("/api/v1/users/import/" + jobId + "/errors")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ERR-USR-01")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bad-email")));
    }
}
