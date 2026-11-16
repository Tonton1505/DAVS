package vn.edu.davs.backend.user;

import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserManagementIntegrationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("davs")
            .withUsername("davs")
            .withPassword("davs_test_pwd");

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
                }

                @Override
                public void send(MimeMessage... mimeMessages) {
                }

                @Override
                public void send(SimpleMailMessage simpleMessage) {
                }

                @Override
                public void send(SimpleMailMessage... simpleMessages) {
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
    private PasswordEncoder passwordEncoder;

    private UUID adminId;
    private String adminToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("UPDATE license_pools SET used = 1, total = 5 WHERE role = 'SITE_ADMIN'");
        jdbcTemplate.update("UPDATE license_pools SET used = 0, total = 10 WHERE role = 'CREATOR'");
        jdbcTemplate.update("UPDATE license_pools SET used = 0, total = 30 WHERE role = 'EXPLORER'");
        jdbcTemplate.update("UPDATE license_pools SET used = 0, total = 200 WHERE role = 'VIEWER'");

        adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE site_role = 'SITE_ADMIN' AND deleted_at IS NULL ORDER BY created_at ASC LIMIT 1",
                UUID.class
        );
        jdbcTemplate.update(
                "UPDATE users SET status = 'ACTIVE', deleted_at = NULL WHERE id = ?",
                adminId
        );
        adminToken = jwtService.generateAccessToken(
                DavsUser.of(adminId, "admin@davs.local", "Admin", SiteRole.SITE_ADMIN)
        );
    }

    @Test
    void UC03_searchFilterChangeRoleAndEnforceLicenseAndOwnershipRules() throws Exception {
        String email = "creator-" + UUID.randomUUID() + "@davs.local";
        MvcResult createRes = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authMethod":"LOCAL","usernames":["%s"],"role":"CREATOR"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdBody = objectMapper.readTree(createRes.getResponse().getContentAsString());
        UUID userId = UUID.fromString(createdBody.path("created").get(0).path("id").asText());

        // Search by query and role
        mockMvc.perform(get("/api/v1/users")
                        .param("q", email)
                        .param("role", "CREATOR")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value(email));

        // Give the Creator an owned project -> downgrading to VIEWER must fail with ERR-USR-06 (BUS-USR-04)
        jdbcTemplate.update(
                "INSERT INTO projects (name, owner_id, permission_mode) VALUES (?, ?, 'CUSTOM')",
                "Proj-" + UUID.randomUUID(),
                userId
        );

        mockMvc.perform(patch("/api/v1/users/" + userId + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"VIEWER"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ERR-USR-06"));

        // Exhaust SITE_ADMIN license pool -> upgrading to SITE_ADMIN fails with ERR-USR-04 (BUS-USR-01)
        jdbcTemplate.update("UPDATE license_pools SET used = 5, total = 5 WHERE role = 'SITE_ADMIN'");
        mockMvc.perform(patch("/api/v1/users/" + userId + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"SITE_ADMIN"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ERR-USR-04"));
    }

    @Test
    void UC03_protectLastSiteAdminAndDeactivateRevokesLoginAndDeleteTransfersOwnership() throws Exception {
        // Delete any other SITE_ADMIN accounts created by other tests so adminId is the sole active admin
        jdbcTemplate.update(
                "UPDATE users SET site_role = 'VIEWER' WHERE site_role = 'SITE_ADMIN' AND id <> ?",
                adminId
        );

        // BUS-USR-05: Cannot deactivate the last active SITE_ADMIN
        mockMvc.perform(post("/api/v1/users/" + adminId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR-PRJ-03"));

        // Create a user with known password, deactivate them, and verify login returns ERR-AUTH-04
        String userEmail = "active-" + UUID.randomUUID() + "@davs.local";
        UUID userId = jdbcTemplate.queryForObject(
                """
                INSERT INTO users (email, password_hash, full_name, auth_method, site_role, status, terms_accepted_at)
                VALUES (?, ?, 'Active User', 'LOCAL', 'CREATOR', 'ACTIVE', now())
                RETURNING id
                """,
                UUID.class,
                userEmail,
                passwordEncoder.encode("Secret@12345")
        );

        mockMvc.perform(post("/api/v1/users/" + userId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Secret@12345","rememberMe":false}
                                """.formatted(userEmail)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-04"));

        // Reactivate user and assign a project to them
        mockMvc.perform(post("/api/v1/users/" + userId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        UUID projectId = jdbcTemplate.queryForObject(
                "INSERT INTO projects (name, owner_id, permission_mode) VALUES (?, ?, 'CUSTOM') RETURNING id",
                UUID.class,
                "OwnedProj-" + UUID.randomUUID(),
                userId
        );

        // [4b] Deleting user with owned content without newOwnerId returns 422 ERR-USR-05
        mockMvc.perform(delete("/api/v1/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ERR-USR-05"));

        // Deleting user with a VIEWER as newOwnerId returns 422 ERR-USR-06
        UUID viewerId = jdbcTemplate.queryForObject(
                """
                INSERT INTO users (email, password_hash, full_name, auth_method, site_role, status)
                VALUES (?, 'hash', 'Viewer User', 'LOCAL', 'VIEWER', 'ACTIVE')
                RETURNING id
                """,
                UUID.class,
                "viewer-" + UUID.randomUUID() + "@davs.local"
        );
        mockMvc.perform(delete("/api/v1/users/" + userId)
                        .param("newOwnerId", viewerId.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ERR-USR-06"));

        // Deleting user with adminId as newOwnerId transfers ownership and deletes user
        mockMvc.perform(delete("/api/v1/users/" + userId)
                        .param("newOwnerId", adminId.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        UUID newProjectOwner = jdbcTemplate.queryForObject(
                "SELECT owner_id FROM projects WHERE id = ?",
                UUID.class,
                projectId
        );
        assertThat(newProjectOwner).isEqualTo(adminId);
    }
}
