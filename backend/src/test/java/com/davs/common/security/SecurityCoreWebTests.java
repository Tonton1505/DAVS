package com.davs.common.security;

import com.davs.common.config.SecurityConfig;
import com.davs.common.security.UserActivityService.UserSessionSnapshot;
import com.davs.common.service.SettingsService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityCoreWebTests.DummySecuredController.class)
@Import({
        SecurityConfig.class,
        JwtService.class,
        JwtAuthenticationFilter.class,
        LastActivityFilter.class,
        AuthRateLimitFilter.class,
        DavsAuthenticationEntryPoint.class,
        DavsAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        SecurityCoreWebTests.DummySecuredController.class,
        SecurityCoreWebTests.StubBeansConfig.class
})
@TestPropertySource(properties = {
        "davs.security.rate-limit.auth-max-requests=5",
        "davs.security.rate-limit.auth-window-seconds=60"
})
class SecurityCoreWebTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StubSettingsService stubSettingsService;

    @Autowired
    private StubUserActivityService stubUserActivityService;

    private DavsUser adminUser;
    private DavsUser viewerUser;

    @BeforeEach
    void setUp() {
        adminUser = DavsUser.of(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "admin@davs.local",
                "Lê Thu Hà",
                SiteRole.SITE_ADMIN
        );
        viewerUser = DavsUser.of(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "viewer@davs.local",
                "Lê Minh Trang",
                SiteRole.VIEWER
        );

        stubSettingsService.setTimeoutMinutes(30);
        stubUserActivityService.clear();
        stubUserActivityService.put(
                adminUser.getId(),
                new UserSessionSnapshot(Instant.now().minusSeconds(60), "ACTIVE")
        );
        stubUserActivityService.put(
                viewerUser.getId(),
                new UserSessionSnapshot(Instant.now().minusSeconds(60), "ACTIVE")
        );
    }

    @Test
    void T005_missingTokenReturns401WithErrAuth05() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-05"));
    }

    @Test
    void T005_invalidSignatureTokenReturns401WithErrAuth05() throws Exception {
        SecurityProperties otherProps = new SecurityProperties();
        otherProps.setJwtSecret(Base64.getEncoder().encodeToString(
                "wrong-secret-key-for-testing-invalid-signature-32bytes".getBytes(StandardCharsets.UTF_8)
        ));
        JwtService wrongKeyJwtService = new JwtService(otherProps);
        String badToken = wrongKeyJwtService.generateAccessToken(adminUser);

        mockMvc.perform(get("/api/v1/test/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + badToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-05"));
    }

    @Test
    void T005_expiredJwtTokenReturns401WithErrAuth05() throws Exception {
        SecurityProperties props = new SecurityProperties();
        props.setJwtSecret("ZGF2cy1kZXYtand0LXNlY3JldC1rZXktbXVzdC1iZS1hdC1sZWFzdC0zMi1ieXRlcy1sb25n");
        props.setAccessTokenTtlMinutes(15L);
        Instant past = Instant.now().minus(Duration.ofMinutes(20));
        JwtService pastJwtService = new JwtService(props, Clock.fixed(past, ZoneOffset.UTC));
        String expiredToken = pastJwtService.generateAccessToken(adminUser);

        mockMvc.perform(get("/api/v1/test/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-05"));
    }

    @Test
    void T005_validJwtReturns200AndSecurityHeaders() throws Exception {
        String token = jwtService.generateAccessToken(adminUser);

        mockMvc.perform(get("/api/v1/test/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@davs.local"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().exists("Referrer-Policy"));
    }

    @Test
    void T005_idleSessionOver30MinutesReturns401ErrAuth05() throws Exception {
        String token = jwtService.generateAccessToken(adminUser);
        stubUserActivityService.put(
                adminUser.getId(),
                new UserSessionSnapshot(Instant.now().minus(Duration.ofMinutes(31)), "ACTIVE")
        );

        mockMvc.perform(get("/api/v1/test/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-05"));
    }

    @Test
    void T005_roleBasedAccessDeniesViewerWith403() throws Exception {
        String viewerToken = jwtService.generateAccessToken(viewerUser);

        mockMvc.perform(get("/api/v1/test/admin-only")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR-PRJ-03"));
    }

    @Test
    void T005_corsAllowsConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/test/protected")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void T005_csrfEnforcedOnCookieRefreshEndpointOnly() throws Exception {
        String token = jwtService.generateAccessToken(adminUser);

        mockMvc.perform(post("/api/v1/test/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isForbidden());
    }

    @Test
    void T005_authRateLimitReturns429AfterLimitExceeded() throws Exception {
        String testIp = "198.51.100.42";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", testIp))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", testIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("ERR-AUTH-RATE-LIMIT"));
    }

    @TestConfiguration
    static class StubBeansConfig {

        @Bean
        @Primary
        StubSettingsService stubSettingsService(SecurityProperties securityProperties) {
            return new StubSettingsService(securityProperties);
        }

        @Bean
        @Primary
        StubUserActivityService stubUserActivityService() {
            return new StubUserActivityService();
        }
    }

    static class StubSettingsService extends SettingsService {
        private int timeoutMinutes = 30;

        StubSettingsService(SecurityProperties securityProperties) {
            super(new JdbcTemplate(), securityProperties);
        }

        void setTimeoutMinutes(int timeoutMinutes) {
            this.timeoutMinutes = timeoutMinutes;
        }

        @Override
        public int getSessionTimeoutMinutes() {
            return timeoutMinutes;
        }

        @Override
        public Duration getSessionTimeout() {
            return Duration.ofMinutes(timeoutMinutes);
        }
    }

    static class StubUserActivityService extends UserActivityService {
        private final Map<UUID, UserSessionSnapshot> store = new ConcurrentHashMap<>();

        StubUserActivityService() {
            super(new JdbcTemplate());
        }

        void clear() {
            store.clear();
        }

        void put(UUID userId, UserSessionSnapshot snapshot) {
            store.put(userId, snapshot);
        }

        @Override
        public Optional<UserSessionSnapshot> findSessionSnapshot(UUID userId) {
            return Optional.ofNullable(store.get(userId));
        }

        @Override
        public void touchLastActivity(UUID userId, Instant activityTime) {
            UserSessionSnapshot current = store.get(userId);
            if (current != null) {
                store.put(userId, new UserSessionSnapshot(activityTime, current.status()));
            }
        }
    }

    @RestController
    static class DummySecuredController {

        @GetMapping("/api/v1/test/protected")
        Map<String, String> protectedGet(@AuthenticationPrincipal DavsUser user) {
            return Map.of("email", user.getEmail());
        }

        @PostMapping("/api/v1/test/protected")
        Map<String, String> protectedPost(@AuthenticationPrincipal DavsUser user) {
            return Map.of("email", user.getEmail());
        }

        @GetMapping("/api/v1/test/admin-only")
        @PreAuthorize("hasRole('SITE_ADMIN')")
        Map<String, String> adminOnly() {
            return Map.of("ok", "true");
        }

        @PostMapping("/api/v1/auth/login")
        Map<String, String> dummyLogin() {
            return Map.of("ok", "true");
        }

        @PostMapping("/api/v1/auth/refresh")
        Map<String, String> dummyRefresh() {
            return Map.of("ok", "true");
        }
    }
}
