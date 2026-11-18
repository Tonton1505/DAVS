package vn.edu.davs.backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.davs.backend.config.SecurityProperties;
import vn.edu.davs.backend.config.SecurityProperties.MfaProperties;
import vn.edu.davs.backend.config.SecurityProperties.OidcProperties;
import vn.edu.davs.backend.dao.JdbcAuthRepository;
import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.AuthTokenResponse;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SettingKey;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.exception.FieldErrorItem;
import vn.edu.davs.backend.model.AuthUserRecord;
import vn.edu.davs.backend.security.JwtService;
import vn.edu.davs.backend.security.JwtService.MfaChallengeClaims;
import vn.edu.davs.backend.security.OidcClient;
import vn.edu.davs.backend.security.OidcClient.OidcUserIdentity;
import vn.edu.davs.backend.security.TotpService;
import vn.edu.davs.backend.service.AuthService.AuthSessionResult;

/**
 * T-102 service for OIDC (Authorization Code + PKCE) login and TOTP Multi-Factor Authentication.
 */
@Service
@RequiredArgsConstructor
public class OidcMfaService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long OIDC_STATE_TTL_SECONDS = 600L;

    private final SecurityProperties securityProperties;
    private final JdbcAuthRepository authRepository;
    private final AuthService authService;
    private final JwtService jwtService;
    private final OidcClient oidcClient;
    private final TotpService totpService;
    private final SecretCryptoService secretCryptoService;
    private final SettingsService settingsService;
    private final AuditService auditService;

    public SsoConfigResponse getSsoConfig() {
        OidcProperties oidc = securityProperties.getOidc();
        MfaProperties mfa = securityProperties.getMfa();
        return new SsoConfigResponse(
                oidc.isEnabled(),
                oidc.getProviderName(),
                mfa.isEnabled(),
                mfa.isRequired()
        );
    }

    @Transactional
    public OidcAuthorizeResponse buildOidcAuthorizeUrl(String requestedRedirectUri) {
        OidcProperties oidc = securityProperties.getOidc();
        if (!oidc.isEnabled() || oidc.getAuthorizationUri() == null || oidc.getAuthorizationUri().isBlank()) {
            throw new DavsException(ErrorCode.ERR_AUTH_04);
        }
        String redirectUri = (requestedRedirectUri != null && !requestedRedirectUri.isBlank())
                ? requestedRedirectUri.trim()
                : oidc.getRedirectUri();
        String state = randomUrlSafe(24);
        String codeVerifier = randomUrlSafe(48);
        String codeChallenge = pkceS256(codeVerifier);

        authRepository.saveOidcAuthState(
                state,
                codeVerifier,
                redirectUri,
                Instant.now().plusSeconds(OIDC_STATE_TTL_SECONDS)
        );

        String sep = oidc.getAuthorizationUri().contains("?") ? "&" : "?";
        String url = oidc.getAuthorizationUri() + sep
                + "response_type=code"
                + "&client_id=" + urlEncode(oidc.getClientId())
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&scope=" + urlEncode(oidc.getScopes())
                + "&state=" + urlEncode(state)
                + "&code_challenge=" + urlEncode(codeChallenge)
                + "&code_challenge_method=S256";
        return new OidcAuthorizeResponse(url, state);
    }

    @Transactional(noRollbackFor = DavsException.class)
    public AuthSessionResult handleOidcCallback(
            String code,
            String state,
            String requestedRedirectUri,
            String clientIp
    ) {
        OidcProperties oidc = securityProperties.getOidc();
        if (!oidc.isEnabled()) {
            auditService.log(AuditEvent.of("AUTH_OIDC_LOGIN_FAILED", "USER", "oidc")
                    .withStates(null, Map.of("reason", "OIDC_DISABLED", "ip", normalizeIp(clientIp))));
            throw new DavsException(ErrorCode.ERR_AUTH_04);
        }

        Instant now = Instant.now();
        Optional<JdbcAuthRepository.OidcAuthStateRecord> stateOpt = authRepository.consumeOidcAuthState(state, now);
        if (stateOpt.isEmpty()) {
            auditService.log(AuditEvent.of("AUTH_OIDC_LOGIN_FAILED", "USER", "oidc")
                    .withStates(null, Map.of("reason", "INVALID_OR_EXPIRED_STATE", "ip", normalizeIp(clientIp))));
            throw new DavsException(ErrorCode.ERR_AUTH_01);
        }

        JdbcAuthRepository.OidcAuthStateRecord stateRec = stateOpt.get();
        String redirectUri = (requestedRedirectUri != null && !requestedRedirectUri.isBlank())
                ? requestedRedirectUri.trim()
                : stateRec.redirectUri();

        OidcUserIdentity identity;
        try {
            identity = oidcClient.exchangeCode(oidc, code, stateRec.codeVerifier(), redirectUri);
        } catch (RuntimeException ex) {
            auditService.log(AuditEvent.of("AUTH_OIDC_LOGIN_FAILED", "USER", "oidc")
                    .withStates(null, Map.of("reason", "TOKEN_EXCHANGE_FAILED", "ip", normalizeIp(clientIp))));
            throw ex;
        }

        Optional<AuthUserRecord> userOpt = authRepository.findActiveUserByEmail(identity.email());
        if (userOpt.isEmpty()) {
            // T-102: Do NOT auto-create accounts for uninvited users
            auditService.log(AuditEvent.of("AUTH_OIDC_LOGIN_FAILED", "USER", identity.email())
                    .withStates(null, Map.of(
                            "email", identity.email(),
                            "reason", "USER_NOT_INVITED",
                            "ip", normalizeIp(clientIp)
                    )));
            throw new DavsException(ErrorCode.ERR_AUTH_04);
        }

        AuthUserRecord user = userOpt.get();
        if (!authService.isUserPermittedToLogin(user)) {
            auditService.log(AuditEvent.of("AUTH_OIDC_LOGIN_FAILED", user.id(), user.email(), "USER", user.id().toString())
                    .withStates(null, Map.of(
                            "reason", "USER_NOT_PERMITTED",
                            "status", user.status(),
                            "ip", normalizeIp(clientIp)
                    )));
            throw new DavsException(ErrorCode.ERR_AUTH_04);
        }

        authRepository.linkOidcSubjectIfEmpty(user.id(), identity.subject(), now);

        if (securityProperties.getMfa().isEnabled() && authRepository.isMfaEnabled(user.id())) {
            String mfaToken = jwtService.generateMfaChallengeToken(user.id(), user.email(), false);
            auditService.log(AuditEvent.of("AUTH_OIDC_MFA_CHALLENGE", user.id(), user.email(), "USER", user.id().toString()));
            return new AuthSessionResult(
                    new AuthTokenResponse(
                            null,
                            0L,
                            !user.termsAccepted(),
                            user.mustChangePassword(),
                            user.toDto(),
                            true,
                            mfaToken
                    ),
                    null,
                    0L
            );
        }

        return authService.issueSessionForUser(user, false, now, "AUTH_OIDC_LOGIN_SUCCESS");
    }

    @Transactional(noRollbackFor = DavsException.class)
    public AuthSessionResult verifyMfaLogin(String mfaToken, String code, String clientIp) {
        MfaChallengeClaims claims = jwtService.parseMfaChallengeToken(mfaToken);
        Instant now = Instant.now();
        String email = claims.email();
        String normalizedIp = normalizeIp(clientIp);

        Optional<JdbcAuthRepository.LoginAttemptState> attemptState = authRepository.findLoginAttempt(email, normalizedIp);
        if (attemptState.isPresent()
                && attemptState.get().lockedUntil() != null
                && attemptState.get().lockedUntil().isAfter(now)) {
            Instant lockedUntil = attemptState.get().lockedUntil();
            auditService.log(AuditEvent.of("AUTH_MFA_LOCKED", claims.userId(), email, "USER", claims.userId().toString())
                    .withStates(null, Map.of("lockedUntil", lockedUntil.toString())));
            throw lockedException(lockedUntil);
        }

        AuthUserRecord user = authRepository.findActiveUserById(claims.userId())
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_AUTH_05));
        if (!authService.isUserPermittedToLogin(user)) {
            throw new DavsException(ErrorCode.ERR_AUTH_04);
        }

        String encryptedSecret = authRepository.findMfaSecretEncrypted(user.id())
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_AUTH_01));
        String secret = secretCryptoService.decrypt(encryptedSecret);

        int drift = securityProperties.getMfa().getAllowedTimeStepDrift();
        if (!totpService.verifyCode(secret, code, now, drift)) {
            int maxFailures = Math.max(1, settingsService.getInt(SettingKey.LOGIN_MAX_FAILURES));
            int lockMinutes = Math.max(1, settingsService.getInt(SettingKey.LOGIN_LOCK_MINUTES));
            JdbcAuthRepository.LoginAttemptState updated = authRepository.recordFailure(
                    email, normalizedIp, maxFailures, lockMinutes, now
            );
            if (updated.lockedUntil() != null) {
                auditService.log(AuditEvent.of("AUTH_MFA_LOCKED", user.id(), email, "USER", user.id().toString())
                        .withStates(null, Map.of(
                                "failedCount", updated.failedCount(),
                                "lockedUntil", updated.lockedUntil().toString()
                        )));
                throw lockedException(updated.lockedUntil());
            }
            auditService.log(AuditEvent.of("AUTH_MFA_FAILED", user.id(), email, "USER", user.id().toString())
                    .withStates(null, Map.of("failedCount", updated.failedCount())));
            throw new DavsException(ErrorCode.ERR_AUTH_01);
        }

        authRepository.clearLoginAttempts(email, normalizedIp);
        return authService.issueSessionForUser(user, claims.rememberMe(), now, "AUTH_MFA_SUCCESS");
    }

    @Transactional(readOnly = true)
    public MfaStatusResponse getMfaStatus(UUID userId) {
        return new MfaStatusResponse(
                authRepository.isMfaEnabled(userId),
                securityProperties.getMfa().isRequired()
        );
    }

    @Transactional
    public MfaSetupResponse setupMfa(UUID userId) {
        AuthUserRecord user = authRepository.findActiveUserById(userId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_AUTH_05));
        String secret = totpService.generateSecret();
        String encrypted = secretCryptoService.encrypt(secret);
        authRepository.savePendingMfaSecret(userId, encrypted, Instant.now());
        String otpauthUri = totpService.buildOtpAuthUri(
                securityProperties.getMfa().getIssuer(),
                user.email(),
                secret
        );
        return new MfaSetupResponse(secret, otpauthUri);
    }

    @Transactional
    public MfaStatusResponse enableMfa(UUID userId, String code) {
        AuthUserRecord user = authRepository.findActiveUserById(userId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_AUTH_05));
        String encryptedSecret = authRepository.findMfaSecretEncrypted(userId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_AUTH_01));
        String secret = secretCryptoService.decrypt(encryptedSecret);
        Instant now = Instant.now();
        if (!totpService.verifyCode(secret, code, now, securityProperties.getMfa().getAllowedTimeStepDrift())) {
            throw new DavsException(ErrorCode.ERR_AUTH_01);
        }
        authRepository.enableMfa(userId, now);
        auditService.log(AuditEvent.of("AUTH_MFA_ENABLED", user.id(), user.email(), "USER", user.id().toString()));
        return new MfaStatusResponse(true, securityProperties.getMfa().isRequired());
    }

    @Transactional
    public MfaStatusResponse disableMfa(UUID userId, String code) {
        if (securityProperties.getMfa().isRequired()) {
            throw new DavsException(ErrorCode.ERR_AUTH_04);
        }
        AuthUserRecord user = authRepository.findActiveUserById(userId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_AUTH_05));
        String encryptedSecret = authRepository.findMfaSecretEncrypted(userId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_AUTH_01));
        String secret = secretCryptoService.decrypt(encryptedSecret);
        Instant now = Instant.now();
        if (!totpService.verifyCode(secret, code, now, securityProperties.getMfa().getAllowedTimeStepDrift())) {
            throw new DavsException(ErrorCode.ERR_AUTH_01);
        }
        authRepository.disableMfa(userId, now);
        auditService.log(AuditEvent.of("AUTH_MFA_DISABLED", user.id(), user.email(), "USER", user.id().toString()));
        return new MfaStatusResponse(false, false);
    }

    private static DavsException lockedException(Instant lockedUntil) {
        return new DavsException(
                ErrorCode.ERR_AUTH_03,
                List.of(new FieldErrorItem("lockedUntil", lockedUntil.toString()))
        );
    }

    private static String normalizeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }
        String trimmed = clientIp.trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }

    private static String randomUrlSafe(int numBytes) {
        byte[] bytes = new byte[numBytes];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String pkceS256(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }

    public record SsoConfigResponse(
            boolean oidcEnabled,
            String providerName,
            boolean mfaEnabled,
            boolean mfaRequired
    ) {
    }

    public record OidcAuthorizeResponse(String authorizationUrl, String state) {
    }

    public record MfaStatusResponse(boolean mfaEnabled, boolean mfaRequired) {
    }

    public record MfaSetupResponse(String secret, String otpauthUri) {
    }
}
