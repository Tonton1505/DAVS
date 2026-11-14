package vn.edu.davs.backend.auth;

import vn.edu.davs.backend.controller.AuthController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTests {

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
    private PasswordEncoder passwordEncoder;

    private String activeEmail;
    private String pendingEmail;
    private String disabledEmail;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM login_attempts");
        String passwordHash = passwordEncoder.encode("StrongPass!123");

        activeEmail = "active-" + UUID.randomUUID() + "@davs.local";
        pendingEmail = "pending-" + UUID.randomUUID() + "@davs.local";
        disabledEmail = "disabled-" + UUID.randomUUID() + "@davs.local";

        jdbcTemplate.update(
                """
                INSERT INTO users (email, password_hash, full_name, site_role, status, terms_accepted_at)
                VALUES (?, ?, 'Active User', 'CREATOR', 'ACTIVE', now())
                """,
                activeEmail,
                passwordHash
        );
        jdbcTemplate.update(
                """
                INSERT INTO users (email, password_hash, full_name, site_role, status, terms_accepted_at)
                VALUES (?, ?, 'Pending User', 'EXPLORER', 'PENDING', NULL)
                """,
                pendingEmail,
                passwordHash
        );
        jdbcTemplate.update(
                """
                INSERT INTO users (email, password_hash, full_name, site_role, status, terms_accepted_at)
                VALUES (?, ?, 'Disabled User', 'VIEWER', 'DISABLED', now())
                """,
                disabledEmail,
                passwordHash
        );
    }

    @Test
    void UC01_loginSuccessMeRefreshRotationAndLogout() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"StrongPass!123","rememberMe":true}
                                """.formatted(activeEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.requiresTermsAcceptance").value(false))
                .andExpect(jsonPath("$.user.email").value(activeEmail))
                .andExpect(jsonPath("$.user.siteRole").value("CREATOR"))
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.path("accessToken").asText();
        Cookie refreshCookie = loginResult.getResponse().getCookie(AuthController.REFRESH_COOKIE_NAME);
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();
        assertThat(refreshCookie.getValue()).isNotBlank();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(activeEmail))
                .andExpect(jsonPath("$.termsAccepted").value(true));

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        Cookie rotatedCookie = refreshResult.getResponse().getCookie(AuthController.REFRESH_COOKIE_NAME);
        assertThat(rotatedCookie).isNotNull();
        assertThat(rotatedCookie.getValue()).isNotEqualTo(refreshCookie.getValue());

        // Old refresh token is revoked after rotation
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-05"));

        // Logout revokes the rotated refresh token
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(rotatedCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(rotatedCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-05"));

        Integer loginAuditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = 'AUTH_LOGIN_SUCCESS' AND actor_email = ?",
                Integer.class,
                activeEmail
        );
        assertThat(loginAuditCount).isEqualTo(1);
    }

    @Test
    void UC01_firstLoginRequiresTermsAndRejectsDecline() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"StrongPass!123","rememberMe":false}
                                """.formatted(pendingEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresTermsAcceptance").value(true))
                .andExpect(jsonPath("$.user.status").value("PENDING"))
                .andExpect(jsonPath("$.user.termsAccepted").value(false))
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("accessToken").asText();

        // [7a] Declining terms returns ERR-AUTH-02 (400)
        mockMvc.perform(post("/api/v1/auth/accept-terms")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accepted\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-02"));

        // Accepting terms activates account
        mockMvc.perform(post("/api/v1/auth/accept-terms")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accepted\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.termsAccepted").value(true));

        // Subsequent login no longer requires terms
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"StrongPass!123","rememberMe":false}
                                """.formatted(pendingEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresTermsAcceptance").value(false))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));
    }

    @Test
    void UC01_validationFailureAndWrongPasswordLockoutAfter5Attempts() throws Exception {
        // [4a] Invalid format returns 400
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\",\"rememberMe\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-01"));

        // [5a] First 4 failed attempts return ERR-AUTH-01 (401)
        for (int attempt = 1; attempt <= 4; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", "203.0.113.10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","password":"WrongPassword1","rememberMe":false}
                                    """.formatted(activeEmail)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("ERR-AUTH-01"));
        }

        // [5b] 5th failed attempt locks for 15 minutes with ERR-AUTH-03 (423)
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"WrongPassword1","rememberMe":false}
                                """.formatted(activeEmail)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-03"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("lockedUntil"));

        // Even with correct password from locked IP, login remains blocked
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"StrongPass!123","rememberMe":false}
                                """.formatted(activeEmail)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-03"));
    }

    @Test
    void UC01_disabledUserReturnsErrAuth04AndUnauthenticatedMeReturnsErrAuth05() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"StrongPass!123","rememberMe":false}
                                """.formatted(disabledEmail)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-04"));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-05"));
    }
}
