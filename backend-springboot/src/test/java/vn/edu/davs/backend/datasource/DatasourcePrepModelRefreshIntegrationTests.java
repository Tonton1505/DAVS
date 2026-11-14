package vn.edu.davs.backend.datasource;

import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.security.JwtService;
import vn.edu.davs.backend.service.JobRunner;
import vn.edu.davs.backend.service.SecretCryptoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DatasourcePrepModelRefreshIntegrationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("davs")
            .withUsername("davs")
            .withPassword("davs_test_pwd");

    private static final AtomicInteger SENT_EMAILS = new AtomicInteger();

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
                    SENT_EMAILS.incrementAndGet();
                }

                @Override
                public void send(MimeMessage... mimeMessages) {
                    SENT_EMAILS.addAndGet(mimeMessages.length);
                }

                @Override
                public void send(SimpleMailMessage simpleMessage) {
                    SENT_EMAILS.incrementAndGet();
                }

                @Override
                public void send(SimpleMailMessage... simpleMessages) {
                    SENT_EMAILS.addAndGet(simpleMessages.length);
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
    private SecretCryptoService secretCryptoService;

    @Autowired
    private JobRunner jobRunner;

    private UUID adminId;
    private String adminToken;

    @BeforeEach
    void setUp() {
        SENT_EMAILS.set(0);
        jdbcTemplate.update("DELETE FROM refresh_runs");
        adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE site_role = 'SITE_ADMIN' AND deleted_at IS NULL LIMIT 1",
                UUID.class
        );
        adminToken = jwtService.generateAccessToken(
                DavsUser.of(adminId, "admin@davs.local", "Admin", SiteRole.SITE_ADMIN)
        );
    }

    @Test
    void UC11_connectorsTestErrorsEncryptedSecretAndCsvUpload() throws Exception {
        mockMvc.perform(get("/api/v1/connectors")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("CSV"))
                .andExpect(jsonPath("$[2].id").value("POSTGRESQL"));

        // ERR-DAT-01: Unreachable host
        mockMvc.perform(post("/api/v1/datasources/test")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "connectorType": "POSTGRESQL",
                                  "host": "loi.congty.vn",
                                  "port": 5432,
                                  "databaseName": "sales",
                                  "username": "u",
                                  "password": "p"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ERR-DAT-01"));

        // ERR-DAT-02: Missing driver
        mockMvc.perform(post("/api/v1/datasources/test")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"connectorType":"SQLSERVER","host":"driver.congty.vn","driverMissing":true}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ERR-DAT-02"));

        // ERR-DAT-03: Invalid credentials
        mockMvc.perform(post("/api/v1/datasources/test")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"connectorType":"POSTGRESQL","host":"pg.congty.vn","username":"bad_user","password":"wrong"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ERR-DAT-03"));

        // BUS-DAT-01 & BUS-DAT-03: Create DB datasource with >1M rows -> suggestedMode = EXTRACT & AES-256-GCM encrypted password
        MvcResult dsRes = mockMvc.perform(post("/api/v1/datasources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Sales_Warehouse_2026",
                                  "connectorType":"POSTGRESQL",
                                  "host":"big.pg.congty.vn",
                                  "port":5432,
                                  "databaseName":"dw",
                                  "username":"reader",
                                  "password":"SuperSecretDbPassword!",
                                  "estimatedRowCount":1500000
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.suggestedMode").value("EXTRACT"))
                .andExpect(jsonPath("$.connection.hasPassword").value(true))
                .andExpect(jsonPath("$.connection.password").doesNotExist())
                .andReturn();

        UUID dsId = UUID.fromString(objectMapper.readTree(dsRes.getResponse().getContentAsString()).path("id").asText());
        String encrypted = jdbcTemplate.queryForObject(
                "SELECT encrypted_secret FROM connections WHERE datasource_id = ?",
                String.class,
                dsId
        );
        assertThat(encrypted).isNotBlank().doesNotContain("SuperSecretDbPassword!");
        assertThat(secretCryptoService.decrypt(encrypted)).isEqualTo("SuperSecretDbPassword!");

        // ERR-DAT-04: Corrupted CSV file rejected
        MockMultipartFile badCsv = new MockMultipartFile(
                "file", "corrupted.csv", "text/csv", new byte[]{0x00, 0x01, 0x02}
        );
        mockMvc.perform(multipart("/api/v1/datasources")
                        .file(badCsv)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR-DAT-04"));
    }

    @Test
    void UC13_semanticDataModelAutoRoleAndRelationshipValidation() throws Exception {
        String csv = "Ma_KH,Doanh_So,Ngay_Dat\nKH01,120000,2026-01-01\nKH02,250000,2026-01-02\n";
        MockMultipartFile validCsv = new MockMultipartFile(
                "file", "don_hang.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );
        MvcResult createRes = mockMvc.perform(multipart("/api/v1/datasources")
                        .file(validCsv)
                        .param("name", "DonHang_CSV")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn();

        UUID dsId = UUID.fromString(objectMapper.readTree(createRes.getResponse().getContentAsString()).path("id").asText());

        // BUS-DAT-08: Ma_KH defaults to DIMENSION, Doanh_So (Number) defaults to MEASURE
        mockMvc.perform(get("/api/v1/datasources/" + dsId + "/model")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields[0].name").value("Ma_KH"))
                .andExpect(jsonPath("$.fields[0].role").value("DIMENSION"))
                .andExpect(jsonPath("$.fields[1].name").value("Doanh_So"))
                .andExpect(jsonPath("$.fields[1].role").value("MEASURE"));

        // [3a] ERR-DAT-06: Incompatible join key types (String vs Number) rejected
        mockMvc.perform(put("/api/v1/datasources/" + dsId + "/model")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "relationships":[
                                    {
                                      "leftTable":"DonHang",
                                      "leftKey":"Ma_KH",
                                      "leftKeyType":"String",
                                      "rightTable":"KhachHang",
                                      "rightKey":"Doanh_So",
                                      "rightKeyType":"Number",
                                      "joinType":"INNER"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ERR-DAT-06"));
    }

    @Test
    void UC12_prepFlowPreviewStepsLineageAndPreserveExtractOnFailure() throws Exception {
        String csv = "Ma_DH,Vung,Doanh_So\nDH01,Miền Bắc,100\nDH01,Miền Bắc,100\nDH02,Miền Nam,\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "sales.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );
        MvcResult dsRes = mockMvc.perform(multipart("/api/v1/datasources")
                        .file(file)
                        .param("name", "Sales_Raw")
                        .param("mode", "EXTRACT")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn();
        UUID dsId = UUID.fromString(objectMapper.readTree(dsRes.getResponse().getContentAsString()).path("id").asText());

        // Create prep flow with REPLACE_NULL + DEDUPE
        MvcResult flowRes = mockMvc.perform(post("/api/v1/prep-flows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "datasourceId":"%s",
                                  "name":"Làm sạch đơn hàng",
                                  "steps":[
                                    {"id":"s1","type":"REPLACE_NULL","name":"Thay Null","config":{"field":"Doanh_So","replacement":0}},
                                    {"id":"s2","type":"DEDUPE","name":"Loại trùng","config":{"field":"Ma_DH"}}
                                  ]
                                }
                                """.formatted(dsId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lineage.length()").value(2))
                .andReturn();
        UUID flowId = UUID.fromString(objectMapper.readTree(flowRes.getResponse().getContentAsString()).path("id").asText());

        // Preview flow returns cleaned rows (3 -> 2 rows after dedupe) and column profiles
        mockMvc.perform(post("/api/v1/prep-flows/" + flowId + "/preview")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputRowCount").value(3))
                .andExpect(jsonPath("$.rowCount").value(2))
                .andExpect(jsonPath("$.removedDuplicates").doesNotExist())
                .andExpect(jsonPath("$.duplicateRowCount").value(1))
                .andExpect(jsonPath("$.replacedNullsCount").value(1));

        // Run flow successfully
        mockMvc.perform(post("/api/v1/prep-flows/" + flowId + "/run")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isAccepted());
        jobRunner.pollScheduled();

        // Trigger a failing run -> status FAILED with ERR-DAT-05 while previous extract row_count (2) stays intact
        mockMvc.perform(post("/api/v1/prep-flows/" + flowId + "/run")
                        .param("simulateFailure", "true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isAccepted());
        jobRunner.pollScheduled();

        Long extractRowCount = jdbcTemplate.queryForObject(
                "SELECT row_count FROM extracts WHERE datasource_id = ?",
                Long.class,
                dsId
        );
        assertThat(extractRowCount).isEqualTo(2L);
    }

    @Test
    void UC14_refreshMaxConcurrentQueueAndAutoPauseAfterFiveFailures() throws Exception {
        MvcResult dsRes = mockMvc.perform(post("/api/v1/datasources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"CRM_Extract",
                                  "connectorType":"POSTGRESQL",
                                  "host":"pg.congty.vn",
                                  "port":5432,
                                  "databaseName":"crm",
                                  "username":"u",
                                  "password":"p",
                                  "mode":"EXTRACT",
                                  "estimatedRowCount":500
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        UUID dsId = UUID.fromString(objectMapper.readTree(dsRes.getResponse().getContentAsString()).path("id").asText());

        // BUS-DAT-12: Enqueue 5 refreshes before running jobs; first 4 are RUNNING, 5th is queued as PENDING
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/v1/datasources/" + dsId + "/refresh")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.status").value("RUNNING"));
        }
        mockMvc.perform(post("/api/v1/datasources/" + dsId + "/refresh")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Clear runs and test BUS-DAT-11: 5 consecutive failures auto-pause schedule and send alert email
        jdbcTemplate.update("DELETE FROM jobs");
        jdbcTemplate.update("DELETE FROM refresh_runs");

        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/datasources/" + dsId + "/refresh")
                            .param("simulateFailure", "true")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isAccepted());
            jobRunner.pollScheduled();
        }

        MvcResult afterFiveFailures = mockMvc.perform(get("/api/v1/datasources/" + dsId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andExpect(jsonPath("$.schedule.paused").value(true))
                .andExpect(jsonPath("$.schedule.consecutiveFailures").value(5))
                .andReturn();

        JsonNode dsNode = objectMapper.readTree(afterFiveFailures.getResponse().getContentAsString());
        assertThat(dsNode.path("rowCount").asLong()).isEqualTo(500L);
        assertThat(SENT_EMAILS.get()).isGreaterThanOrEqualTo(5);
    }
}
