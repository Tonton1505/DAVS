package vn.edu.davs.backend.share;


import vn.edu.davs.backend.service.impl.ConnectedAppServiceImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.davs.backend.dto.ConnectedAppDto;
import vn.edu.davs.backend.dto.ConnectedAppTokenRequest;
import vn.edu.davs.backend.dto.ConnectedAppTokenResponse;
import vn.edu.davs.backend.dto.CreateConnectedAppRequest;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.service.ConnectedAppService;
import vn.edu.davs.backend.service.PermissionService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class ConnectedAppServiceTests {

    private JdbcTemplate jdbcTemplate;
    private PermissionService permissionService;
    private AuditService auditService;
    private ObjectMapper objectMapper;
    private ConnectedAppService connectedAppService;

    private DavsUser adminUser;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        permissionService = mock(PermissionService.class);
        auditService = mock(AuditService.class);
        objectMapper = new ObjectMapper();
        connectedAppService = new ConnectedAppServiceImpl(jdbcTemplate, permissionService, auditService, objectMapper);

        adminUser = DavsUser.of(
                UUID.randomUUID(),
                "admin@congty.vn",
                "Admin User",
                SiteRole.SITE_ADMIN
        );
    }

    @Test
    void testCreateDirectTrustAppSuccess() {
        CreateConnectedAppRequest req = new CreateConnectedAppRequest(
                "Internal Portal",
                "DIRECT_TRUST",
                List.of("portal.congty.vn", "sub.congty.vn"),
                null,
                null,
                "ALL",
                null
        );

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("Internal Portal"))).thenReturn(0);

        ConnectedAppDto dto = connectedAppService.createConnectedApp(req, adminUser);

        assertNotNull(dto);
        assertEquals("Internal Portal", dto.name());
        assertEquals("DIRECT_TRUST", dto.trustType());
        assertNotNull(dto.clientId());
        assertTrue(dto.clientId().startsWith("davs_"));
        assertNotNull(dto.clientSecret());
        assertTrue(dto.clientSecret().startsWith("sec_"));
        verify(auditService).log(any());
    }

    @Test
    void testCreateDuplicateNameThrowsConflict() {
        CreateConnectedAppRequest req = new CreateConnectedAppRequest(
                "Duplicate App",
                "DIRECT_TRUST",
                List.of("portal.congty.vn"),
                null,
                null,
                "ALL",
                null
        );

        // Name exists (ERR-INT-03)
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("Duplicate App"))).thenReturn(1);

        DavsException ex = assertThrows(DavsException.class, () ->
                connectedAppService.createConnectedApp(req, adminUser)
        );
        assertEquals(ErrorCode.ERR_INT_03, ex.getErrorCode());
    }

    @Test
    void testCreateInvalidDomainThrowsBadRequest() {
        CreateConnectedAppRequest req = new CreateConnectedAppRequest(
                "Bad Domain App",
                "DIRECT_TRUST",
                List.of("https://invalid-protocol.com"), // domain shouldn't have protocol (ERR-INT-02)
                null,
                null,
                "ALL",
                null
        );

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("Bad Domain App"))).thenReturn(0);

        DavsException ex = assertThrows(DavsException.class, () ->
                connectedAppService.createConnectedApp(req, adminUser)
        );
        assertEquals(ErrorCode.ERR_INT_02, ex.getErrorCode());
    }

    @Test
    void testRotateSecretInvalidatesOldAndReturnsNew() {
        UUID appId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(appId)))
                .thenReturn(new ConnectedAppDto(
                        appId, "Portal", "DIRECT_TRUST", "ENABLED", "davs_client_1", null,
                        List.of("portal.congty.vn"), null, null, "ALL", null, null, null, null
                ));

        ConnectedAppDto rotated = connectedAppService.rotateSecret(appId, adminUser);

        assertNotNull(rotated);
        assertNotNull(rotated.clientSecret());
        assertTrue(rotated.clientSecret().startsWith("sec_"));
        verify(jdbcTemplate).update(eq("UPDATE connected_app_tokens SET revoked = TRUE WHERE app_id = ?"), eq(appId));
        verify(auditService).log(any());
    }

    @Test
    void testGenerateTokenSuccessWithMatchingDomain() throws NoSuchAlgorithmException {
        String clientId = "davs_client_123";
        String rawSecret = "sec_test_secret_123";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String secretHash = Base64.getEncoder().encodeToString(digest.digest(rawSecret.getBytes(StandardCharsets.UTF_8)));

        when(jdbcTemplate.queryForMap(anyString(), eq(clientId))).thenReturn(Map.of(
                "id", UUID.randomUUID(),
                "trust_type", "DIRECT_TRUST",
                "status", "ENABLED",
                "client_secret_hash", secretHash,
                "allowed_domains", "[\"portal.congty.vn\"]",
                "run_as_user_id", UUID.randomUUID()
        ));

        ConnectedAppTokenRequest req = new ConnectedAppTokenRequest(
                clientId,
                rawSecret,
                "portal.congty.vn"
        );

        ConnectedAppTokenResponse res = connectedAppService.generateToken(req);

        assertNotNull(res);
        assertNotNull(res.accessToken());
        assertEquals("Bearer", res.tokenType());
        assertEquals("DIRECT_TRUST", res.trustType());
    }

    @Test
    void testGenerateTokenMismatchDomainFails() throws NoSuchAlgorithmException {
        String clientId = "davs_client_123";
        String rawSecret = "sec_test_secret_123";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String secretHash = Base64.getEncoder().encodeToString(digest.digest(rawSecret.getBytes(StandardCharsets.UTF_8)));

        when(jdbcTemplate.queryForMap(anyString(), eq(clientId))).thenReturn(Map.of(
                "id", UUID.randomUUID(),
                "trust_type", "DIRECT_TRUST",
                "status", "ENABLED",
                "client_secret_hash", secretHash,
                "allowed_domains", "[\"portal.congty.vn\"]",
                "run_as_user_id", UUID.randomUUID()
        ));

        ConnectedAppTokenRequest req = new ConnectedAppTokenRequest(
                clientId,
                rawSecret,
                "hacker.badsite.com" // Mismatched domain (BUS-INT-05 / ERR-INT-02)
        );

        DavsException ex = assertThrows(DavsException.class, () ->
                connectedAppService.generateToken(req)
        );
        assertEquals(ErrorCode.ERR_INT_02, ex.getErrorCode());
    }
}
