package com.davs.project;

import com.davs.common.security.DavsUser;
import com.davs.common.security.JwtService;
import com.davs.common.security.SiteRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProjectCreationIntegrationTests {

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

    private UUID creatorId;
    private String creatorToken;
    private String explorerToken;

    @BeforeEach
    void setUp() {
        creatorId = UUID.randomUUID();
        String creatorEmail = "creator-" + creatorId + "@davs.local";
        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password_hash, full_name, site_role, status, terms_accepted_at)
                VALUES (?, ?, '$2a$10$dummyhashdummyhashdummyhashdummyhashdummyhashdummyha', 'Creator', 'CREATOR', 'ACTIVE', now())
                """,
                creatorId,
                creatorEmail
        );
        creatorToken = jwtService.generateAccessToken(
                DavsUser.of(creatorId, creatorEmail, "Creator", SiteRole.CREATOR)
        );
        explorerToken = jwtService.generateAccessToken(
                DavsUser.of(UUID.randomUUID(), "explorer@davs.local", "Explorer", SiteRole.EXPLORER)
        );
    }

    @Test
    void UC05_viewerOrExplorerCannotCreateProjectReturnsErrPrj03() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + explorerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Explorer Project","permissionMode":"CUSTOM"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR-PRJ-03"));
    }

    @Test
    void UC05_nameValidationBoundary1And128And129AndForbiddenChars() throws Exception {
        // [4a] Blank name -> ERR-PRJ-02
        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   ","permissionMode":"CUSTOM"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR-PRJ-02"));

        // [4a] Forbidden character -> ERR-PRJ-02
        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Sales/2026","permissionMode":"CUSTOM"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR-PRJ-02"));

        // [4a] 129 chars -> ERR-PRJ-02
        String name129 = "A".repeat(129);
        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","permissionMode":"CUSTOM"}
                                """.formatted(name129)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR-PRJ-02"));

        // 128 chars -> 201 Created
        String name128 = "B".repeat(125) + UUID.randomUUID().toString().substring(0, 3);
        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","permissionMode":"CUSTOM"}
                                """.formatted(name128)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name128));
    }

    @Test
    void UC05_duplicateSiblingReturnsErrPrj01AndMaxDepth5Enforced() throws Exception {
        String rootName = "Root-" + UUID.randomUUID();
        MvcResult rootRes = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":"Top level","permissionMode":"CUSTOM"}
                                """.formatted(rootName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.depth").value(1))
                .andExpect(jsonPath("$.ownerId").value(creatorId.toString()))
                .andReturn();

        String currentParentId = objectMapper.readTree(rootRes.getResponse().getContentAsString())
                .path("id").asText();

        // [4b] Case-insensitive duplicate sibling -> 409 ERR-PRJ-01
        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","permissionMode":"CUSTOM"}
                                """.formatted(rootName.toLowerCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ERR-PRJ-01"));

        // Nest levels 2..5
        for (int depth = 2; depth <= 5; depth++) {
            MvcResult childRes = mockMvc.perform(post("/api/v1/projects")
                            .header("Authorization", "Bearer " + creatorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Level-%d","parentId":"%s","permissionMode":"INHERIT"}
                                    """.formatted(depth, currentParentId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.depth").value(depth))
                    .andReturn();
            currentParentId = objectMapper.readTree(childRes.getResponse().getContentAsString())
                    .path("id").asText();
        }

        // Level 6 -> rejected per BUS-PRJ-02
        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Level-6","parentId":"%s","permissionMode":"INHERIT"}
                                """.formatted(currentParentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR-PRJ-02"));

        mockMvc.perform(get("/api/v1/projects/" + currentParentId)
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depth").value(5));

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = 'PROJECT_CREATE' AND actor_id = ?",
                Integer.class,
                creatorId
        );
        assertThat(auditCount).isEqualTo(5);
    }
}
