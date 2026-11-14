package com.davs.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.davs.common.security.DavsUser;
import com.davs.common.security.SiteRole;
import com.davs.permission.PermissionMode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ResourcePermissionsIntegrationTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("davs_test")
            .withUsername("davs")
            .withPassword("davs");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID insertUser(String email, SiteRole role) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password_hash, full_name, auth_method, site_role, status)
                VALUES (?, ?, '$2a$10$dummyhashdummyhashdummyhashdummyhashdummyhashdummyha', ?, 'LOCAL', ?, 'ACTIVE')
                """,
                id,
                email,
                email,
                role.name()
        );
        return id;
    }

    private UUID insertGroup(String name) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO groups (id, name) VALUES (?, ?)", id, name);
        return id;
    }

    private void addGroupMember(UUID groupId, UUID userId) {
        jdbcTemplate.update(
                "INSERT INTO group_members (group_id, user_id) VALUES (?, ?)",
                groupId,
                userId
        );
    }

    private UUID insertProject(String name, UUID parentId, UUID ownerId, PermissionMode mode) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO projects (id, name, parent_id, owner_id, permission_mode, is_default)
                VALUES (?, ?, ?, ?, ?, FALSE)
                """,
                id,
                name,
                parentId,
                ownerId,
                mode.name()
        );
        return id;
    }

    @Test
    void uc07TemplatesAndUpdatePermissionsWithAuditLog() throws Exception {
        UUID creatorId = insertUser("uc07-creator1@davs.local", SiteRole.CREATOR);
        UUID viewerId = insertUser("uc07-viewer1@davs.local", SiteRole.VIEWER);
        UUID projectId = insertProject("UC07 Project 1", null, creatorId, PermissionMode.CUSTOM);
        DavsUser creator = DavsUser.of(creatorId, "uc07-creator1@davs.local", "Creator 1", SiteRole.CREATOR);

        mockMvc.perform(get("/api/v1/permission-templates").with(user(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        mockMvc.perform(get("/api/v1/resources/project/" + projectId + "/permissions").with(user(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canEdit").value(true))
                .andExpect(jsonPath("$.lockedByParent").value(false));

        String payload = """
                {
                  "rules": [
                    { "subjectType": "USER", "subjectId": "%s", "capability": "VIEW", "effect": "ALLOW" },
                    { "subjectType": "USER", "subjectId": "%s", "capability": "FILTER", "effect": "ALLOW" }
                  ]
                }
                """.formatted(viewerId, viewerId);

        mockMvc.perform(put("/api/v1/resources/project/" + projectId + "/permissions")
                        .with(user(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings.length()").value(0))
                .andExpect(jsonPath("$.permissions.rules.length()").value(2));

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = 'PERMISSION_UPDATE' AND resource_id = ?",
                Integer.class,
                projectId.toString()
        );
        assertThat(auditCount).isEqualTo(1);
    }

    @Test
    void uc07DenyOverridesAllowAndMultiGroupEffectivePermissions() throws Exception {
        UUID creatorId = insertUser("uc07-creator2@davs.local", SiteRole.CREATOR);
        UUID explorerId = insertUser("uc07-explorer2@davs.local", SiteRole.EXPLORER);
        UUID groupAllowId = insertGroup("UC07 Group Allow " + UUID.randomUUID());
        UUID groupDenyId = insertGroup("UC07 Group Deny " + UUID.randomUUID());
        addGroupMember(groupAllowId, explorerId);
        addGroupMember(groupDenyId, explorerId);

        UUID projectId = insertProject("UC07 Project 2", null, creatorId, PermissionMode.CUSTOM);
        DavsUser creator = DavsUser.of(creatorId, "uc07-creator2@davs.local", "Creator 2", SiteRole.CREATOR);

        // Both groups + conflicting ALLOW/DENY on USER VIEW (DENY must win in payload too)
        String payload = """
                {
                  "rules": [
                    { "subjectType": "GROUP", "subjectId": "%s", "capability": "VIEW", "effect": "ALLOW" },
                    { "subjectType": "GROUP", "subjectId": "%s", "capability": "DOWNLOAD", "effect": "ALLOW" },
                    { "subjectType": "GROUP", "subjectId": "%s", "capability": "DOWNLOAD", "effect": "DENY" }
                  ]
                }
                """.formatted(groupAllowId, groupAllowId, groupDenyId);

        mockMvc.perform(put("/api/v1/resources/project/" + projectId + "/permissions")
                        .with(user(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/resources/project/" + projectId + "/permissions/effective")
                        .param("userId", explorerId.toString())
                        .with(user(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveCapabilities").value(org.hamcrest.Matchers.hasItem("VIEW")))
                .andExpect(jsonPath("$.effectiveCapabilities").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("DOWNLOAD"))));
    }

    @Test
    void uc07RoleCeilingWarningStillSavesAndCapsEffectivePermissions() throws Exception {
        UUID creatorId = insertUser("uc07-creator3@davs.local", SiteRole.CREATOR);
        UUID viewerId = insertUser("uc07-viewer3@davs.local", SiteRole.VIEWER);
        UUID projectId = insertProject("UC07 Project 3", null, creatorId, PermissionMode.CUSTOM);
        DavsUser creator = DavsUser.of(creatorId, "uc07-creator3@davs.local", "Creator 3", SiteRole.CREATOR);

        String payload = """
                {
                  "rules": [
                    { "subjectType": "USER", "subjectId": "%s", "capability": "VIEW", "effect": "ALLOW" },
                    { "subjectType": "USER", "subjectId": "%s", "capability": "WEB_EDIT", "effect": "ALLOW" }
                  ]
                }
                """.formatted(viewerId, viewerId);

        mockMvc.perform(put("/api/v1/resources/project/" + projectId + "/permissions")
                        .with(user(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings[0]").value(org.hamcrest.Matchers.containsString("bị giới hạn bởi vai trò")))
                .andExpect(jsonPath("$.permissions.rules.length()").value(2));

        mockMvc.perform(get("/api/v1/resources/project/" + projectId + "/permissions/effective")
                        .param("userId", viewerId.toString())
                        .with(user(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveCapabilities").value(org.hamcrest.Matchers.hasItem("VIEW")))
                .andExpect(jsonPath("$.effectiveCapabilities").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("WEB_EDIT"))));
    }

    @Test
    void uc07LockedParentProjectReturns409AndReadOnly() throws Exception {
        UUID creatorId = insertUser("uc07-creator4@davs.local", SiteRole.CREATOR);
        UUID viewerId = insertUser("uc07-viewer4@davs.local", SiteRole.VIEWER);
        UUID parentId = insertProject("UC07 Locked Parent", null, creatorId, PermissionMode.LOCKED);
        UUID childId = insertProject("UC07 Locked Child", parentId, creatorId, PermissionMode.INHERIT);
        DavsUser creator = DavsUser.of(creatorId, "uc07-creator4@davs.local", "Creator 4", SiteRole.CREATOR);

        mockMvc.perform(get("/api/v1/resources/project/" + childId + "/permissions").with(user(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockedByParent").value(true))
                .andExpect(jsonPath("$.canEdit").value(false));

        String payload = """
                {
                  "rules": [
                    { "subjectType": "USER", "subjectId": "%s", "capability": "VIEW", "effect": "ALLOW" }
                  ]
                }
                """.formatted(viewerId);

        mockMvc.perform(put("/api/v1/resources/project/" + childId + "/permissions")
                        .with(user(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DIS-PRJ-03"));
    }

    @Test
    void uc07ActorWithoutChangePermissionsReturnsErrPrj03() throws Exception {
        UUID ownerId = insertUser("uc07-owner5@davs.local", SiteRole.CREATOR);
        UUID otherCreatorId = insertUser("uc07-other5@davs.local", SiteRole.CREATOR);
        UUID viewerId = insertUser("uc07-viewer5@davs.local", SiteRole.VIEWER);
        UUID projectId = insertProject("UC07 Project 5", null, ownerId, PermissionMode.CUSTOM);
        DavsUser otherCreator = DavsUser.of(otherCreatorId, "uc07-other5@davs.local", "Other", SiteRole.CREATOR);

        String payload = """
                {
                  "rules": [
                    { "subjectType": "USER", "subjectId": "%s", "capability": "VIEW", "effect": "ALLOW" }
                  ]
                }
                """.formatted(viewerId);

        mockMvc.perform(put("/api/v1/resources/project/" + projectId + "/permissions")
                        .with(user(otherCreator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR-PRJ-03"));
    }
}
