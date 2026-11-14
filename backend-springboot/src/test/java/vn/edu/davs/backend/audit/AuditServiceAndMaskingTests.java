package vn.edu.davs.backend.audit;

import vn.edu.davs.backend.dao.AuditLogStore;
import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.AuditLogFilter;
import vn.edu.davs.backend.dto.AuditLogResponse;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.service.SensitiveDataMasker;

import vn.edu.davs.backend.dto.PageResponse;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.enu.SiteRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class AuditServiceAndMaskingTests {

    private SensitiveDataMasker masker;
    private CapturingAuditLogStore store;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        masker = new SensitiveDataMasker(new ObjectMapper());
        store = new CapturingAuditLogStore();
        auditService = new AuditService(store, masker);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void T007_maskerRedactsNestedPasswordsTokensAndSecrets() {
        Map<String, Object> payload = Map.of(
                "email", "an@davs.local",
                "password", "SuperSecret123!",
                "passwordHash", "$2a$10$hash",
                "auth", Map.of(
                        "accessToken", "eyJhbGciOiJIUzI1NiJ9.payload.sig",
                        "refreshToken", "rt-123456",
                        "api_key", "key-999",
                        "role", "CREATOR"
                ),
                "headers", List.of(
                        Map.of("authorization", "Bearer abc", "xRequestId", "req-1")
                )
        );

        JsonNode masked = masker.mask(payload);

        assertThat(masked.get("email").asText()).isEqualTo("an@davs.local");
        assertThat(masked.get("password").asText()).isEqualTo(SensitiveDataMasker.REDACTED);
        assertThat(masked.get("passwordHash").asText()).isEqualTo(SensitiveDataMasker.REDACTED);
        assertThat(masked.get("auth").get("accessToken").asText()).isEqualTo(SensitiveDataMasker.REDACTED);
        assertThat(masked.get("auth").get("refreshToken").asText()).isEqualTo(SensitiveDataMasker.REDACTED);
        assertThat(masked.get("auth").get("api_key").asText()).isEqualTo(SensitiveDataMasker.REDACTED);
        assertThat(masked.get("auth").get("role").asText()).isEqualTo("CREATOR");
        assertThat(masked.get("headers").get(0).get("authorization").asText()).isEqualTo(SensitiveDataMasker.REDACTED);
        assertThat(masked.get("headers").get(0).get("xRequestId").asText()).isEqualTo("req-1");
    }

    @Test
    void T007_auditServiceEnrichesActorFromSecurityContextAndMasksStates() {
        UUID actorId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        DavsUser principal = DavsUser.of(actorId, "admin@davs.local", "Lê Thu Hà", SiteRole.SITE_ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        AuditEvent event = AuditEvent.of("USER_CREATE", "USER", "u-1")
                .withStates(
                        null,
                        Map.of("email", "new@davs.local", "password", "PlainPass!", "token", "secret-tok")
                );

        auditService.log(event);

        assertThat(store.inserted).hasSize(1);
        InsertedRow row = store.inserted.get(0);
        assertThat(row.action).isEqualTo("USER_CREATE");
        assertThat(row.actorId).isEqualTo(actorId);
        assertThat(row.actorEmail).isEqualTo("admin@davs.local");
        assertThat(row.afterState.get("email").asText()).isEqualTo("new@davs.local");
        assertThat(row.afterState.get("password").asText()).isEqualTo(SensitiveDataMasker.REDACTED);
        assertThat(row.afterState.get("token").asText()).isEqualTo(SensitiveDataMasker.REDACTED);
    }

    private record InsertedRow(
            String action,
            UUID actorId,
            String actorEmail,
            JsonNode beforeState,
            JsonNode afterState
    ) {
    }

    private static final class CapturingAuditLogStore implements AuditLogStore {
        private final List<InsertedRow> inserted = new ArrayList<>();

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
            inserted.add(new InsertedRow(action, actorId, actorEmail, beforeState, afterState));
        }

        @Override
        public PageResponse<AuditLogResponse> search(AuditLogFilter filter, Pageable pageable) {
            return new PageResponse<>(List.of(), 0, 25, 0);
        }
    }
}
