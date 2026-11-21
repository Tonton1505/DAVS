package vn.edu.davs.backend.service;

import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.service.AuthService.AuthSessionResult;

/**
 * T-102 service for OIDC (Authorization Code + PKCE) login and TOTP Multi-Factor Authentication.
 */
public interface OidcMfaService {


    SsoConfigResponse getSsoConfig();


    @Transactional
    OidcAuthorizeResponse buildOidcAuthorizeUrl(String requestedRedirectUri);


    @Transactional(noRollbackFor = DavsException.class)
    AuthSessionResult handleOidcCallback(
            String code,
            String state,
            String requestedRedirectUri,
            String clientIp
    );


    @Transactional(noRollbackFor = DavsException.class)
    AuthSessionResult verifyMfaLogin(String mfaToken, String code, String clientIp);


    @Transactional(readOnly = true)
    MfaStatusResponse getMfaStatus(UUID userId);


    @Transactional
    MfaSetupResponse setupMfa(UUID userId);


    @Transactional
    MfaStatusResponse enableMfa(UUID userId, String code);


    @Transactional
    MfaStatusResponse disableMfa(UUID userId, String code);


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
