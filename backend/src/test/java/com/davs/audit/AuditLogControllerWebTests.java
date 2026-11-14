package com.davs.audit;

import com.davs.common.api.PageResponse;
import com.davs.common.config.SecurityConfig;
import com.davs.common.security.AuthRateLimitFilter;
import com.davs.common.security.DavsAccessDeniedHandler;
import com.davs.common.security.DavsAuthenticationEntryPoint;
import com.davs.common.security.DavsUser;
import com.davs.common.security.JwtAuthenticationFilter;
import com.davs.common.security.JwtService;
import com.davs.common.security.LastActivityFilter;
import com.davs.common.security.SecurityErrorResponseWriter;
import com.davs.common.security.SecurityProperties;
import com.davs.common.security.SiteRole;
import com.davs.common.security.UserActivityService;
import com.davs.common.service.SettingsService;
import com.davs.permission.PermissionService;
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
            };
        }

        @Bean
        @Primary
        com.davs.permission.PermissionRuleStore stubPermissionRuleStore() {
            return (resource, userId, groupIds) -> List.of();
        }

        @Bean
        @Primary
        com.davs.permission.GroupMembershipProvider stubGroupMembershipProvider() {
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
