package vn.edu.davs.backend.integration;

import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.security.JwtService;
import vn.edu.davs.backend.service.SecretCryptoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ExternalIntegrationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("davs")
            .withUsername("davs")
            .withPassword("davs_test_pwd");

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

    private String adminToken;
    private String creatorToken;

    @BeforeEach
    void setUp() {
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
    void UC25_nonAdminForbiddenAndOAuthFlowEncryptsTokenAndHandlesExpiredAndDisconnect() throws Exception {
        // BUS-INT-01: Non-admin forbidden
        mockMvc.perform(get("/api/v1/integrations")
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR-PRJ-03"));

        // Initiate Slack OAuth connect -> returns authorizeUrl and state
        MvcResult initRes = mockMvc.perform(post("/api/v1/integrations/SLACK/connect")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platform").value("SLACK"))
                .andExpect(jsonPath("$.authorizeUrl").isNotEmpty())
                .andExpect(jsonPath("$.state").isNotEmpty())
                .andReturn();

        String state = objectMapper.readTree(initRes.getResponse().getContentAsString()).path("state").asText();

        // [4a] Denied callback returns 422 ERR-INT-01
        mockMvc.perform(get("/api/v1/integrations/SLACK/callback")
                        .param("error", "access_denied")
                        .param("state", state)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ERR-INT-01"));

        // Valid callback encrypts token in DB and never exposes raw/encrypted token in API response
        mockMvc.perform(get("/api/v1/integrations/SLACK/callback")
                        .param("code", "valid-auth-code-123")
                        .param("state", state)
                        .param("accountName", "workspace@congty.vn")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONNECTED"))
                .andExpect(jsonPath("$.hasToken").value(true))
                .andExpect(jsonPath("$.encryptedAccessToken").doesNotExist());

        String encryptedToken = jdbcTemplate.queryForObject(
                "SELECT encrypted_access_token FROM integrations WHERE platform = 'SLACK'",
                String.class
        );
        assertThat(encryptedToken).isNotBlank().doesNotContain("valid-auth-code-123");
        assertThat(secretCryptoService.decrypt(encryptedToken)).contains("valid-auth-code-123");

        // [5b] Expired token automatically transitions to RECONNECT_REQUIRED
        jdbcTemplate.update(
                "UPDATE integrations SET token_expires_at = now() - interval '1 hour' WHERE platform = 'SLACK'"
        );
        MvcResult listRes = mockMvc.perform(get("/api/v1/integrations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = objectMapper.readTree(listRes.getResponse().getContentAsString());
        assertThat(list.get(0).path("status").asText()).isEqualTo("RECONNECT_REQUIRED");

        // [5a] & BUS-INT-03: Disconnect revokes token and sets status DISCONNECTED
        mockMvc.perform(delete("/api/v1/integrations/SLACK")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISCONNECTED"))
                .andExpect(jsonPath("$.hasToken").value(false));
    }
}
