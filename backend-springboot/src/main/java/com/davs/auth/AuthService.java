package com.davs.auth;

import com.davs.audit.AuditEvent;
import com.davs.audit.AuditService;
import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.common.error.FieldErrorItem;
import com.davs.common.security.DavsUser;
import com.davs.common.security.JwtService;
import com.davs.common.security.SecurityProperties;
import com.davs.common.service.SettingKey;
import com.davs.common.service.SettingsService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core authentication use cases for UC01: login, lockout, terms acceptance, refresh, logout, and /me.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Dummy BCrypt hash used when email is unknown so login verification runs in constant time.
     */
    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiH/6W4E1I4wG7k3yN8wV1H2u8W7l2K";
    private static final long DEFAULT_REFRESH_TTL_DAYS = 1L;
    private static final long REMEMBER_ME_REFRESH_TTL_DAYS = 30L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JdbcAuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityProperties securityProperties;
    private final SettingsService settingsService;
    private final AuditService auditService;

    @Transactional(noRollbackFor = DavsException.class)
    public AuthSessionResult login(LoginRequest request, String clientIp) {
        Instant now = Instant.now();
        String email = request.email().trim();
        String normalizedIp = normalizeIp(clientIp);

        Optional<JdbcAuthRepository.LoginAttemptState> attemptState = authRepository.findLoginAttempt(email, normalizedIp);
        if (attemptState.isPresent()
                && attemptState.get().lockedUntil() != null
                && attemptState.get().lockedUntil().isAfter(now)) {
            Instant lockedUntil = attemptState.get().lockedUntil();
            auditService.log(AuditEvent.of("AUTH_LOGIN_LOCKED", "USER", email)
                    .withStates(null, Map.of("email", email, "lockedUntil", lockedUntil.toString())));
            throw lockedException(lockedUntil);
        }

        Optional<AuthUserRecord> userOpt = authRepository.findActiveUserByEmail(email);
        String hashToVerify = userOpt.map(AuthUserRecord::passwordHash).orElse(DUMMY_BCRYPT_HASH);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToVerify);

        if (userOpt.isEmpty() || !passwordMatches) {
            int maxFailures = Math.max(1, settingsService.getInt(SettingKey.LOGIN_MAX_FAILURES));
            int lockMinutes = Math.max(1, settingsService.getInt(SettingKey.LOGIN_LOCK_MINUTES));
            JdbcAuthRepository.LoginAttemptState updated = authRepository.recordFailure(
                    email, normalizedIp, maxFailures, lockMinutes, now
            );
            if (updated.lockedUntil() != null) {
                auditService.log(AuditEvent.of("AUTH_LOGIN_LOCKED", "USER", email)
                        .withStates(null, Map.of(
                                "email", email,
                                "failedCount", updated.failedCount(),
                                "lockedUntil", updated.lockedUntil().toString()
                        )));
                throw lockedException(updated.lockedUntil());
            }
            auditService.log(AuditEvent.of("AUTH_LOGIN_FAILED", "USER", email)
                    .withStates(null, Map.of("email", email, "failedCount", updated.failedCount())));
            throw new DavsException(ErrorCode.ERR_AUTH_01);
        }

        AuthUserRecord user = userOpt.get();
        if (!isUserPermittedToLogin(user)) {
            auditService.log(AuditEvent.of("AUTH_LOGIN_DENIED", user.id(), user.email(), "USER", user.id().toString())
                    .withStates(null, Map.of("status", user.status(), "siteRole", user.siteRole().name())));
            throw new DavsException(ErrorCode.ERR_AUTH_04);
        }

        authRepository.clearLoginAttempts(email, normalizedIp);
        authRepository.markLoginTimestamps(user.id(), now);

        AuthTokenPair tokens = issueTokens(user, request.rememberMe(), now);
        auditService.log(AuditEvent.of("AUTH_LOGIN_SUCCESS", user.id(), user.email(), "USER", user.id().toString())
                .withStates(null, Map.of(
                        "rememberMe", request.rememberMe(),
                        "requiresTermsAcceptance", !user.termsAccepted()
                )));
        return new AuthSessionResult(
                new AuthTokenResponse(
                        tokens.accessToken(),
                        tokens.accessTtlSeconds(),
                        !user.termsAccepted(),
                        user.mustChangePassword(),
                        user.toDto()
                ),
                tokens.rawRefreshToken(),
                tokens.refreshTtlSeconds()
        );
    }

    @Transactional
    public AuthUserDto acceptTerms(UUID userId, AcceptTermsRequest request) {
        if (!request.accepted()) {
            throw new DavsException(ErrorCode.ERR_AUTH_02);
        }
        AuthUserRecord before = authRepository.findActiveUserById(userId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_AUTH_05));
        if (!isUserPermittedToLogin(before)) {
            throw new DavsException(ErrorCode.ERR_AUTH_04);
        }

        Instant now = Instant.now();
        authRepository.acceptTermsAndActivate(userId, now);
        AuthUserRecord after = authRepository.findActiveUserById(userId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_AUTH_05));

        auditService.log(AuditEvent.of("AUTH_TERMS_ACCEPTED", after.id(), after.email(), "USER", after.id().toString())
                .withStates(
                        Map.of("status", before.status(), "termsAccepted", before.termsAccepted()),
                        Map.of("status", after.status(), "termsAccepted", after.termsAccepted())
                ));
        return after.toDto();
    }

    @Transactional
    public AuthSessionResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new DavsException(ErrorCode.ERR_AUTH_05);
        }
        Instant now = Instant.now();
        String tokenHash = sha256Hex(rawRefreshToken);
        JdbcAuthRepository.RefreshTokenRecord existing = authRepository.findRefreshToken(tokenHash)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_AUTH_05));

        if (!existing.isActive(now)) {
            throw new DavsException(ErrorCode.ERR_AUTH_05);
        }

        AuthUserRecord user = authRepository.findActiveUserById(existing.userId())
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_AUTH_05));
        if (!isUserPermittedToLogin(user)) {
            throw new DavsException(ErrorCode.ERR_AUTH_04);
        }

        authRepository.revokeRefreshToken(tokenHash, now);
        AuthTokenPair rotated = issueTokens(user, existing.rememberMe(), now);
        return new AuthSessionResult(
                new AuthTokenResponse(
                        rotated.accessToken(),
                        rotated.accessTtlSeconds(),
                        !user.termsAccepted(),
                        user.mustChangePassword(),
                        user.toDto()
                ),
                rotated.rawRefreshToken(),
                rotated.refreshTtlSeconds()
        );
    }

    @Transactional
    public void logout(String rawRefreshToken, DavsUser principal) {
        Instant now = Instant.now();
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            authRepository.revokeRefreshToken(sha256Hex(rawRefreshToken), now);
        }
        if (principal != null) {
            auditService.log(AuditEvent.of(
                    "AUTH_LOGOUT",
                    principal.getId(),
                    principal.getEmail(),
                    "USER",
                    principal.getId().toString()
            ));
        }
    }

    @Transactional(readOnly = true)
    public AuthUserDto getCurrentUser(UUID userId) {
        AuthUserRecord user = authRepository.findActiveUserById(userId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_AUTH_05));
        if (!isUserPermittedToLogin(user)) {
            throw new DavsException(ErrorCode.ERR_AUTH_04);
        }
        return user.toDto();
    }

    private boolean isUserPermittedToLogin(AuthUserRecord user) {
        if ("DISABLED".equals(user.status())
                || "LOCKED".equals(user.status())
                || "UNINVITED".equals(user.status())) {
            return false;
        }
        return authRepository.hasValidLicensePool(user.siteRole());
    }

    private AuthTokenPair issueTokens(AuthUserRecord user, boolean rememberMe, Instant now) {
        DavsUser principal = DavsUser.of(user.id(), user.email(), user.fullName(), user.siteRole());
        String accessToken = jwtService.generateAccessToken(principal);
        long accessTtlSeconds = securityProperties.getAccessTokenTtlMinutes() * 60L;

        String rawRefreshToken = generateOpaqueToken();
        long refreshTtlDays = rememberMe ? REMEMBER_ME_REFRESH_TTL_DAYS : DEFAULT_REFRESH_TTL_DAYS;
        long refreshTtlSeconds = refreshTtlDays * 24L * 3600L;
        Instant refreshExpiresAt = now.plusSeconds(refreshTtlSeconds);

        authRepository.saveRefreshToken(
                user.id(),
                sha256Hex(rawRefreshToken),
                rememberMe,
                refreshExpiresAt,
                now
        );
        return new AuthTokenPair(accessToken, accessTtlSeconds, rawRefreshToken, refreshTtlSeconds);
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

    private static String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    private record AuthTokenPair(
            String accessToken,
            long accessTtlSeconds,
            String rawRefreshToken,
            long refreshTtlSeconds
    ) {
    }

    public record AuthSessionResult(
            AuthTokenResponse response,
            String rawRefreshToken,
            long refreshTtlSeconds
    ) {
    }
}
