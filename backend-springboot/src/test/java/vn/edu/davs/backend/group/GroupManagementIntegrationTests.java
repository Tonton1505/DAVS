package vn.edu.davs.backend.group;

import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.security.JwtService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class GroupManagementIntegrationTests {

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

    private String adminToken;
    private String viewerToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("UPDATE license_pools SET used = 0, total = 30 WHERE role = 'EXPLORER'");
        jdbcTemplate.update("UPDATE license_pools SET used = 0, total = 200 WHERE role = 'VIEWER'");

        UUID adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE site_role = 'SITE_ADMIN' AND deleted_at IS NULL LIMIT 1",
                UUID.class
        );
        adminToken = jwtService.generateAccessToken(
                DavsUser.of(adminId, "admin@davs.local", "Admin", SiteRole.SITE_ADMIN)
        );
        viewerToken = jwtService.generateAccessToken(
                DavsUser.of(UUID.randomUUID(), "viewer@davs.local", "Viewer", SiteRole.VIEWER)
        );
    }

    @Test
    void UC04_nonAdminIsForbiddenAndDuplicateOrProtectedAllUsersGroupRejected() throws Exception {
        mockMvc.perform(get("/api/v1/groups")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR-PRJ-03"));

        // Duplicate "All Users" rejected with 409 ERR-GRP-01
        mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"all users","description":"dup"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ERR-GRP-01"));

        // BUS-GRP-02: Deleting "All Users" is forbidden
        UUID allUsersId = jdbcTemplate.queryForObject(
                "SELECT id FROM groups WHERE lower(name) = lower('All Users')",
                UUID.class
        );
        mockMvc.perform(delete("/api/v1/groups/" + allUsersId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR-PRJ-03"));
    }

    @Test
    void UC04_createGroupAddMembersPromotesMinRoleAndDeleteRevokesPermissions() throws Exception {
        UUID viewerUserId = jdbcTemplate.queryForObject(
                """
                INSERT INTO users (email, password_hash, full_name, auth_method, site_role, status)
                VALUES (?, 'hash', 'Member One', 'LOCAL', 'VIEWER', 'ACTIVE')
                RETURNING id
                """,
                UUID.class,
                "grp-member-" + UUID.randomUUID() + "@davs.local"
        );

        String groupName = "Sales-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult createRes = mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":"Nhóm kinh doanh","minRole":"EXPLORER"}
                                """.formatted(groupName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(groupName))
                .andExpect(jsonPath("$.minRole").value("EXPLORER"))
                .andReturn();

        UUID groupId = UUID.fromString(
                objectMapper.readTree(createRes.getResponse().getContentAsString()).path("id").asText()
        );

        // [6] Adding VIEWER member to group with minRole=EXPLORER upgrades user to EXPLORER
        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userIds":["%s"]}
                                """.formatted(viewerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.members[0].siteRole").value("EXPLORER"));

        String promotedRole = jdbcTemplate.queryForObject(
                "SELECT site_role FROM users WHERE id = ?",
                String.class,
                viewerUserId
        );
        assertThat(promotedRole).isEqualTo("EXPLORER");

        // [6a] When EXPLORER license pool is full, adding another VIEWER returns 422 ERR-USR-04
        jdbcTemplate.update("UPDATE license_pools SET used = 30, total = 30 WHERE role = 'EXPLORER'");
        UUID viewer2Id = jdbcTemplate.queryForObject(
                """
                INSERT INTO users (email, password_hash, full_name, auth_method, site_role, status)
                VALUES (?, 'hash', 'Member Two', 'LOCAL', 'VIEWER', 'ACTIVE')
                RETURNING id
                """,
                UUID.class,
                "grp-member2-" + UUID.randomUUID() + "@davs.local"
        );
        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userIds":["%s"]}
                                """.formatted(viewer2Id)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ERR-USR-04"));

        // Add a permission rule for this group, then delete group -> rule revoked, user account preserved
        UUID defaultProjId = jdbcTemplate.queryForObject(
                "SELECT id FROM projects WHERE lower(name) = lower('Default') LIMIT 1",
                UUID.class
        );
        jdbcTemplate.update(
                """
                INSERT INTO permission_rules (subject_type, subject_id, resource_type, resource_id, capability, effect)
                VALUES ('GROUP', ?, 'PROJECT', ?, 'VIEW', 'ALLOW')
                """,
                groupId,
                defaultProjId
        );

        mockMvc.perform(delete("/api/v1/groups/" + groupId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        Integer remainingRules = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM permission_rules WHERE subject_type = 'GROUP' AND subject_id = ?",
                Integer.class,
                groupId
        );
        assertThat(remainingRules).isZero();

        Integer userStillExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ? AND deleted_at IS NULL",
                Integer.class,
                viewerUserId
        );
        assertThat(userStillExists).isEqualTo(1);
    }
}
