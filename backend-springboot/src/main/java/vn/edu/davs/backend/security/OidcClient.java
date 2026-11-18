package vn.edu.davs.backend.security;

import vn.edu.davs.backend.config.SecurityProperties.OidcProperties;

/**
 * Abstraction for OIDC Authorization Code + PKCE token exchange and userinfo retrieval (T-102).
 */
public interface OidcClient {

    /**
     * Exchanges an authorization code and PKCE code_verifier for the authenticated OIDC user identity.
     */
    OidcUserIdentity exchangeCode(OidcProperties oidc, String code, String codeVerifier, String redirectUri);

    /**
     * Verified user identity returned by the OIDC Identity Provider.
     */
    record OidcUserIdentity(String subject, String email, boolean emailVerified) {
    }
}
