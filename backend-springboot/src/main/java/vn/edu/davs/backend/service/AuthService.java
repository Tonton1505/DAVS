package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.AcceptTermsRequest;
import vn.edu.davs.backend.dto.AuthTokenResponse;
import vn.edu.davs.backend.dto.AuthUserDto;
import vn.edu.davs.backend.dto.LoginRequest;
import vn.edu.davs.backend.model.AuthUserRecord;
import vn.edu.davs.backend.common.exception.DavsException;
import vn.edu.davs.backend.model.DavsUser;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core authentication use cases for UC01: login, lockout, terms acceptance, refresh, logout, and /me.
 */
public interface AuthService {


    @Transactional(noRollbackFor = DavsException.class)
    AuthSessionResult login(LoginRequest request, String clientIp);


    AuthSessionResult issueSessionForUser(
            AuthUserRecord user,
            boolean rememberMe,
            Instant now,
            String auditAction
    );


    @Transactional
    AuthUserDto acceptTerms(UUID userId, AcceptTermsRequest request);


    @Transactional
    AuthSessionResult refresh(String rawRefreshToken);


    @Transactional
    void logout(String rawRefreshToken, DavsUser principal);


    @Transactional(readOnly = true)
    AuthUserDto getCurrentUser(UUID userId);


    boolean isUserPermittedToLogin(AuthUserRecord user);


    public record AuthSessionResult(
            AuthTokenResponse response,
            String rawRefreshToken,
            long refreshTtlSeconds
    ) {
    }
}
