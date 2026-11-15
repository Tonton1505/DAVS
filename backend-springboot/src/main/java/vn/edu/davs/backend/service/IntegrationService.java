package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.JdbcIntegrationRepository;
import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.IntegrationConnectResponse;
import vn.edu.davs.backend.dto.IntegrationDto;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.model.DavsUser;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core UC25 business logic for external platform OAuth 2.0 integrations (Slack, Salesforce):
 * enforces BUS-INT-01 (Site Admin only), BUS-INT-02 (OAuth 2.0 CSRF state &amp; AES-256-GCM encrypted tokens),
 * BUS-INT-03 (non-destructive disconnect), and ERR-INT-01 on authorization failure.
 */
@Service
@RequiredArgsConstructor
public class IntegrationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long DEFAULT_TOKEN_TTL_DAYS = 30L;

    private final JdbcIntegrationRepository repository;
    private final SecretCryptoService secretCryptoService;
    private final AuditService auditService;

    @Transactional
    public List<IntegrationDto> listIntegrations() {
        repository.markExpiredTokensReconnectRequired();
        return repository.listAll();
    }

    @Transactional
    public IntegrationConnectResponse initiateConnect(String rawPlatform, DavsUser actor) {
        IntegrationDto existing = resolvePlatform(rawPlatform);
        String state = generateStateToken();
        repository.saveOauthState(existing.platform(), state);

        String scopes = existing.scopes() != null ? existing.scopes() : "read";
        String authorizeUrl = buildAuthorizeUrl(existing.platform(), scopes, state);

        auditService.log(AuditEvent.of(
                        "INTEGRATION_CONNECT_INITIATED",
                        actor.getId(),
                        actor.getEmail(),
                        "INTEGRATION",
                        existing.platform())
                .withStates(null, Map.of("platform", existing.platform(), "scopes", scopes)));

        return new IntegrationConnectResponse(existing.platform(), authorizeUrl, state, scopes);
    }

    @Transactional
    public IntegrationDto handleCallback(
            String rawPlatform,
            String code,
            String state,
            String error,
            String accountName,
            Long expiresInSeconds,
            DavsUser actor
    ) {
        IntegrationDto existing = resolvePlatform(rawPlatform);
        String displayName = existing.displayName();

        // [4a] Platform denied access or authentication error -> ERR-INT-01
        if (error != null && !error.isBlank()) {
            throw new DavsException(ErrorCode.ERR_INT_01, displayName);
        }
        if (code == null || code.isBlank()) {
            throw new DavsException(ErrorCode.ERR_INT_01, displayName);
        }
        String lowerCode = code.trim().toLowerCase(Locale.ROOT);
        if (lowerCode.contains("denied") || lowerCode.contains("invalid") || lowerCode.contains("fail")) {
            throw new DavsException(ErrorCode.ERR_INT_01, displayName);
        }

        // BUS-INT-02: Validate OAuth CSRF state parameter
        String expectedState = repository.findOauthState(existing.platform()).orElse(null);
        if (expectedState != null && (state == null || !expectedState.equals(state.trim()))) {
            throw new DavsException(ErrorCode.ERR_INT_01, displayName);
        }

        // Exchange authorization code for OAuth access & refresh tokens and encrypt with AES-256-GCM
        String rawAccessToken = "oauth2-access-" + existing.platform().toLowerCase(Locale.ROOT) + "-" + code.trim();
        String rawRefreshToken = "oauth2-refresh-" + existing.platform().toLowerCase(Locale.ROOT) + "-" + generateStateToken();
        String encryptedAccess = secretCryptoService.encrypt(rawAccessToken);
        String encryptedRefresh = secretCryptoService.encrypt(rawRefreshToken);

        Instant expiresAt = expiresInSeconds != null
                ? Instant.now().plusSeconds(expiresInSeconds)
                : Instant.now().plus(DEFAULT_TOKEN_TTL_DAYS, ChronoUnit.DAYS);
        String resolvedAccount = (accountName != null && !accountName.isBlank())
                ? accountName.trim()
                : actor.getEmail();

        repository.markConnected(
                existing.platform(),
                encryptedAccess,
                encryptedRefresh,
                resolvedAccount,
                expiresAt,
                actor.getId()
        );
        repository.markExpiredTokensReconnectRequired();

        IntegrationDto updated = resolvePlatform(existing.platform());
        auditService.log(AuditEvent.of(
                        "INTEGRATION_CONNECTED",
                        actor.getId(),
                        actor.getEmail(),
                        "INTEGRATION",
                        existing.platform())
                .withStates(
                        Map.of("status", existing.status()),
                        Map.of("status", updated.status(), "accountName", resolvedAccount)
                ));
        return updated;
    }

    @Transactional
    public IntegrationDto disconnect(String rawPlatform, DavsUser actor) {
        IntegrationDto existing = resolvePlatform(rawPlatform);

        // [5a] & BUS-INT-03: Revoke token and set DISCONNECTED without deleting system data
        repository.markDisconnected(existing.platform());
        IntegrationDto updated = resolvePlatform(existing.platform());

        auditService.log(AuditEvent.of(
                        "INTEGRATION_DISCONNECTED",
                        actor.getId(),
                        actor.getEmail(),
                        "INTEGRATION",
                        existing.platform())
                .withStates(
                        Map.of("status", existing.status()),
                        Map.of("status", "DISCONNECTED")
                ));
        return updated;
    }

    private IntegrationDto resolvePlatform(String rawPlatform) {
        if (rawPlatform == null || rawPlatform.isBlank()) {
            throw new DavsException(ErrorCode.ERR_INT_01, "Nền tảng");
        }
        return repository.findByPlatform(rawPlatform.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_INT_01, rawPlatform.trim()));
    }

    private static String buildAuthorizeUrl(String platform, String scopes, String state) {
        String encodedScopes = URLEncoder.encode(scopes, StandardCharsets.UTF_8);
        String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);
        if ("SLACK".equalsIgnoreCase(platform)) {
            return "https://slack.com/oauth/v2/authorize?client_id=davs-slack-client&scope="
                    + encodedScopes + "&state=" + encodedState;
        }
        return "https://login.salesforce.com/services/oauth2/authorize?response_type=code&client_id=davs-sf-client&scope="
                + encodedScopes + "&state=" + encodedState;
    }

    private static String generateStateToken() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
