package vn.edu.davs.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.edu.davs.backend.security.OidcClient;
import vn.edu.davs.backend.security.OidcClient.OidcUserIdentity;
import vn.edu.davs.backend.security.TotpService;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
        "davs.security.oidc.enabled=true",
        "davs.security.oidc.provider-name=TestIdP",
        "davs.security.oidc.client-id=davs-client",
        "davs.security.oidc.authorization-uri=https://idp.example.test/oauth2/authorize",
        "davs.security.oidc.token-uri=https://idp.example.test/oauth2/token",
        "davs.security.oidc.redirect-uri=http://localhost:3000/login/oidc/callback",
        "davs.security.mfa.enabled=true",
        "davs.security.mfa.required=false",
        "davs.security.rate-limit.auth-max-requests=100"
})
class OidcAndMfaIntegrationTests {

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

    @Autowired
    private TotpService totpService;

    @MockBean
    private OidcClient oidcClient;

    private String invitedEmail;
    private String mfaUserEmail;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        invitedEmail = "oidc-" + suffix + "@davs.local";
        mfaUserEmail = "mfa-" + suffix + "@davs.local";
        String passwordHash = passwordEncoder.encode("StrongPass@123");

        jdbcTemplate.update(
                """
                INSERT INTO users (email, password_hash, full_name, auth_method, site_role, status, terms_accepted_at)
                VALUES (?, ?, ?, 'SSO', 'CREATOR', 'ACTIVE', now()),
                       (?, ?, ?, 'LOCAL', 'EXPLORER', 'ACTIVE', now())
                """,
                invitedEmail, passwordHash, "OIDC User",
                mfaUserEmail, passwordHash, "MFA User"
        );
    }

    @Test
    void oidcAuthorizeGeneratesPkceAndCallbackLinksInvitedUserWithAudit() throws Exception {
        MvcResult authzRes = mockMvc.perform(get("/api/v1/auth/oidc/authorize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").isNotEmpty())
                .andExpect(jsonPath("$.state").isNotEmpty())
                .andReturn();

        JsonNode authzJson = objectMapper.readTree(authzRes.getResponse().getContentAsString());
        String authzUrl = authzJson.get("authorizationUrl").asText();
        String state = authzJson.get("state").asText();
        assertThat(authzUrl).contains("code_challenge_method=S256").contains("code_challenge=");

        when(oidcClient.exchangeCode(any(), eq("auth-code-123"), any(), any()))
                .thenReturn(new OidcUserIdentity("sub-" + invitedEmail, invitedEmail, true));

        mockMvc.perform(post("/api/v1/auth/oidc/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CallbackPayload("auth-code-123", state, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(invitedEmail));

        String subject = jdbcTemplate.queryForObject(
                "SELECT oidc_subject FROM users WHERE email = ?",
                String.class,
                invitedEmail
        );
        assertThat(subject).isEqualTo("sub-" + invitedEmail);

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = 'AUTH_OIDC_LOGIN_SUCCESS' AND actor_email = ?",
                Integer.class,
                invitedEmail
        );
        assertThat(auditCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void oidcRejectsUninvitedEmailWithoutCreatingAccountAndAuditsFailure() throws Exception {
        MvcResult authzRes = mockMvc.perform(get("/api/v1/auth/oidc/authorize"))
                .andExpect(status().isOk())
                .andReturn();
        String state = objectMapper.readTree(authzRes.getResponse().getContentAsString()).get("state").asText();
        String uninvited = "stranger-" + UUID.randomUUID().toString().substring(0, 8) + "@outside.test";

        when(oidcClient.exchangeCode(any(), eq("auth-code-stranger"), any(), any()))
                .thenReturn(new OidcUserIdentity("sub-stranger", uninvited, true));

        mockMvc.perform(post("/api/v1/auth/oidc/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CallbackPayload("auth-code-stranger", state, null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-04"));

        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE lower(email) = lower(?)",
                Integer.class,
                uninvited
        );
        assertThat(userCount).isZero();

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = 'AUTH_OIDC_LOGIN_FAILED' AND resource_id = ?",
                Integer.class,
                uninvited
        );
        assertThat(auditCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void mfaSetupEnableVerifyAndLockoutAfterFiveWrongCodes() throws Exception {
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + mfaUserEmail + "\",\"password\":\"StrongPass@123\",\"rememberMe\":false}"))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(loginRes.getResponse().getContentAsString())
                .get("accessToken").asText();

        MvcResult setupRes = mockMvc.perform(post("/api/v1/auth/mfa/setup")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").isNotEmpty())
                .andExpect(jsonPath("$.otpauthUri").isNotEmpty())
                .andReturn();
        String secret = objectMapper.readTree(setupRes.getResponse().getContentAsString())
                .get("secret").asText();

        String validCode = totpService.generateCode(secret, Instant.now());
        mockMvc.perform(post("/api/v1/auth/mfa/enable")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + validCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaEnabled").value(true));

        MvcResult challengeRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + mfaUserEmail + "\",\"password\":\"StrongPass@123\",\"rememberMe\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andExpect(jsonPath("$.mfaToken").isNotEmpty())
                .andReturn();
        String mfaToken = objectMapper.readTree(challengeRes.getResponse().getContentAsString())
                .get("mfaToken").asText();

        // Wrong code 4 times -> 401 ERR-AUTH-01
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/v1/auth/mfa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mfaToken\":\"" + mfaToken + "\",\"code\":\"000000\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("ERR-AUTH-01"));
        }

        // 5th wrong code -> 423 ERR-AUTH-03 (BUS-AUTH-02 lockout)
        mockMvc.perform(post("/api/v1/auth/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mfaToken\":\"" + mfaToken + "\",\"code\":\"000000\"}"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-03"));

        Integer lockAudit = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = 'AUTH_MFA_LOCKED' AND actor_email = ?",
                Integer.class,
                mfaUserEmail
        );
        assertThat(lockAudit).isGreaterThanOrEqualTo(1);
    }

    private record CallbackPayload(String code, String state, String redirectUri) {
    }
}
