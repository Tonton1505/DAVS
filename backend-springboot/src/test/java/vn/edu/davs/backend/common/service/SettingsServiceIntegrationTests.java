package vn.edu.davs.backend.common.service;

import vn.edu.davs.backend.enu.SettingKey;
import vn.edu.davs.backend.service.SettingsService;

import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.security.JwtService;
import vn.edu.davs.backend.enu.SiteRole;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SettingsServiceIntegrationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("davs")
            .withUsername("davs")
            .withPassword("davs_test_pwd");

    private static final UUID ADMIN_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String viewerToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, full_name, site_role, status) "
                        + "VALUES (?, 'admin-settings@davs.local', '$2a$10$dummyhash', 'Admin', 'SITE_ADMIN', 'ACTIVE') "
                        + "ON CONFLICT (id) DO NOTHING",
                ADMIN_ID
        );

        DavsUser admin = DavsUser.of(ADMIN_ID, "admin-settings@davs.local", "Admin", SiteRole.SITE_ADMIN);
        DavsUser viewer = DavsUser.of(UUID.randomUUID(), "viewer-settings@davs.local", "Viewer", SiteRole.VIEWER);
        adminToken = jwtService.generateAccessToken(admin);
        viewerToken = jwtService.generateAccessToken(viewer);
        settingsService.evictCache();
    }

    @Test
    void T008_all17DefaultSettingsAreSeededAndTypedReadersWork() {
        assertThat(settingsService.getInt(SettingKey.SESSION_IDLE_MINUTES)).isEqualTo(30);
        assertThat(settingsService.getInt(SettingKey.LOGIN_MAX_FAILURES)).isEqualTo(5);
        assertThat(settingsService.getInt(SettingKey.LOGIN_LOCK_MINUTES)).isEqualTo(15);
        assertThat(settingsService.getLong(SettingKey.UPLOAD_MAX_BYTES)).isEqualTo(1_073_741_824L);
        assertThat(settingsService.getInt(SettingKey.WORKBOOK_MAX_VERSIONS)).isEqualTo(25);
        assertThat(settingsService.getInt(SettingKey.EXPORT_MAX_ROWS)).isEqualTo(500_000);
        assertThat(settingsService.getLong(SettingKey.EXPORT_MAX_BYTES)).isEqualTo(104_857_600L);
        assertThat(settingsService.getInt(SettingKey.CHART_MAX_MARKS)).isEqualTo(100_000);
        assertThat(settingsService.getInt(SettingKey.TRASH_RETENTION_DAYS)).isEqualTo(30);
        assertThat(settingsService.getInt(SettingKey.AUDIT_RETENTION_MONTHS)).isEqualTo(12);
        assertThat(settingsService.getInt(SettingKey.IMPORT_MAX_USERS_PER_FILE)).isEqualTo(1000);
        assertThat(settingsService.getInt(SettingKey.REFRESH_MAX_CONCURRENT)).isEqualTo(4);
        assertThat(settingsService.getInt(SettingKey.REFRESH_PAUSE_AFTER_FAILURES)).isEqualTo(5);
        assertThat(settingsService.getInt(SettingKey.COMMENT_EDIT_WINDOW_HOURS)).isEqualTo(24);
        assertThat(settingsService.getInt(SettingKey.SHARE_MAX_RECIPIENTS)).isEqualTo(100);
        assertThat(settingsService.getInt(SettingKey.PROJECT_MAX_DEPTH)).isEqualTo(5);
        assertThat(settingsService.getBoolean(SettingKey.PUBLISH_PUBLIC_ENABLED)).isFalse();
    }

    @Test
    void T008_adminSettingsEndpointsRequireSiteAdminAndApplyChangesImmediately() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-05"));

        mockMvc.perform(get("/api/v1/admin/settings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR-PRJ-03"));

        mockMvc.perform(get("/api/v1/admin/settings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(17));

        // Update values via PUT and verify immediate effect without restart
        mockMvc.perform(put("/api/v1/admin/settings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "settings": {
                                    "session.idleMinutes": "45",
                                    "publish.publicEnabled": "true"
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(settingsService.getSessionTimeout()).isEqualTo(Duration.ofMinutes(45));
        assertThat(settingsService.getBoolean(SettingKey.PUBLISH_PUBLIC_ENABLED)).isTrue();

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = 'SYSTEM_SETTINGS_UPDATE' AND actor_id = ?",
                Integer.class,
                ADMIN_ID
        );
        assertThat(auditCount).isGreaterThanOrEqualTo(1);

        // Invalid value returns 400
        mockMvc.perform(put("/api/v1/admin/settings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "settings": {
                                    "session.idleMinutes": "-10"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
