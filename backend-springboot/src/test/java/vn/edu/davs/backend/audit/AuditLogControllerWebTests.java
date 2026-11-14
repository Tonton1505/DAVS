package vn.edu.davs.backend.audit;

import vn.edu.davs.backend.controller.AuditLogController;
import vn.edu.davs.backend.dao.AuditLogStore;
import vn.edu.davs.backend.dto.AuditLogFilter;
import vn.edu.davs.backend.dto.AuditLogResponse;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.service.SensitiveDataMasker;

import vn.edu.davs.backend.dto.PageResponse;
import vn.edu.davs.backend.config.SecurityConfig;
import vn.edu.davs.backend.security.AuthRateLimitFilter;
import vn.edu.davs.backend.security.DavsAccessDeniedHandler;
import vn.edu.davs.backend.security.DavsAuthenticationEntryPoint;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.security.JwtAuthenticationFilter;
import vn.edu.davs.backend.security.JwtService;
import vn.edu.davs.backend.security.LastActivityFilter;
import vn.edu.davs.backend.security.SecurityErrorResponseWriter;
import vn.edu.davs.backend.config.SecurityProperties;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.security.UserActivityService;
import vn.edu.davs.backend.service.SettingsService;
import vn.edu.davs.backend.service.PermissionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuditLogController.class)
@Import({
        SecurityConfig.class,
        JwtService.class,
        JwtAuthenticationFilter.class,
        LastActivityFilter.class,
        AuthRateLimitFilter.class,
        DavsAuthenticationEntryPoint.class,
        DavsAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        PermissionService.class,
        AuditService.class,
        SensitiveDataMasker.class,
        AuditLogControllerWebTests.StubConfig.class
})
class AuditLogControllerWebTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StubAuditLogStore stubAuditLogStore;

    @Test
    void T007_unauthenticatedRequestReturns401ErrAuth05() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-05"));
    }

    @Test
    void T007_nonSiteAdminReturns403ErrPrj03() throws Exception {
        DavsUser creator = DavsUser.of(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "creator@davs.local",
                "Nguyễn Văn An",
                SiteRole.CREATOR
        );
        String token = jwtService.generateAccessToken(creator);

        mockMvc.perform(get("/api/v1/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR-PRJ-03"));
    }

    @Test
    void T007_siteAdminCanFilterAndPaginateAuditLogs() throws Exception {
        DavsUser admin = DavsUser.of(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "admin@davs.local",
                "Lê Thu Hà",
                SiteRole.SITE_ADMIN
        );
        String token = jwtService.generateAccessToken(admin);

        mockMvc.perform(get("/api/v1/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("actorEmail", "an@davs.local")
                        .param("action", "USER_LOGIN")
                        .param("from", "2026-11-01T00:00:00Z")
                        .param("to", "2026-11-14T23:59:59Z")
                        .param("page", "1")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(25))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].action").value("USER_LOGIN"))
                .andExpect(jsonPath("$.content[0].actorEmail").value("an@davs.local"));

        assertThat(stubAuditLogStore.lastFilter.actorEmail()).isEqualTo("an@davs.local");
        assertThat(stubAuditLogStore.lastFilter.action()).isEqualTo("USER_LOGIN");
        assertThat(stubAuditLogStore.lastFilter.from()).isEqualTo(Instant.parse("2026-11-01T00:00:00Z"));
        assertThat(stubAuditLogStore.lastFilter.to()).isEqualTo(Instant.parse("2026-11-14T23:59:59Z"));
        assertThat(stubAuditLogStore.lastPageable.getPageNumber()).isEqualTo(1);
        assertThat(stubAuditLogStore.lastPageable.getPageSize()).isEqualTo(25);
    }

    @TestConfiguration
    static class StubConfig {

        @Bean
        @Primary
        StubAuditLogStore stubAuditLogStore() {
            return new StubAuditLogStore();
        }

        @Bean
        @Primary
        SettingsService stubSettingsService(SecurityProperties props) {
            return new SettingsService(new JdbcTemplate(), props) {
                @Override
                public Duration getSessionTimeout() {
                    return Duration.ofMinutes(30);
                }
            };
        }

        @Bean
        @Primary
        UserActivityService stubUserActivityService() {
            return new UserActivityService(new JdbcTemplate()) {
                @Override
                public Optional<UserSessionSnapshot> findSessionSnapshot(UUID userId) {
                    return Optional.of(new UserSessionSnapshot(Instant.now().minusSeconds(60), "ACTIVE"));
                }

                @Override
                public void touchLastActivity(UUID userId, Instant activityTime) {
                }
            };
        }

        @Bean
        @Primary
        vn.edu.davs.backend.dao.PermissionRuleStore stubPermissionRuleStore() {
            return (resource, userId, groupIds) -> List.of();
        }

        @Bean
        @Primary
        vn.edu.davs.backend.dao.GroupMembershipProvider stubGroupMembershipProvider() {
            return userId -> Set.of();
        }
    }

    static class StubAuditLogStore implements AuditLogStore {
        AuditLogFilter lastFilter;
        Pageable lastPageable;

        @Override
        public void insert(
                String action,
                UUID actorId,
                String actorEmail,
                String resourceType,
                String resourceId,
                JsonNode beforeState,
                JsonNode afterState,
                String ipAddress,
                String userAgent,
                String traceId
        ) {
        }

        @Override
        public PageResponse<AuditLogResponse> search(AuditLogFilter filter, Pageable pageable) {
            this.lastFilter = filter;
            this.lastPageable = pageable;
            AuditLogResponse item = new AuditLogResponse(
                    UUID.fromString("99999999-9999-9999-9999-999999999999"),
                    "USER_LOGIN",
                    UUID.fromString("22222222-2222-2222-2222-222222222222"),
                    "an@davs.local",
                    "AUTH",
                    "session-1",
                    null,
                    new ObjectMapper().createObjectNode().put("result", "SUCCESS"),
                    "127.0.0.1",
                    "JUnit",
                    "trace-1",
                    Instant.parse("2026-11-14T02:00:00Z")
            );
            return new PageResponse<>(List.of(item), pageable.getPageNumber(), pageable.getPageSize(), 1L);
        }
    }
}
