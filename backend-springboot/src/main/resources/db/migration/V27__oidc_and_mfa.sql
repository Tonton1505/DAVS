-- ============================================================================
-- T-102: SSO/OIDC (Authorization Code + PKCE) and TOTP MFA support
-- ============================================================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS mfa_secret_encrypted TEXT,
    ADD COLUMN IF NOT EXISTS mfa_verified_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS oidc_subject VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_oidc_subject
    ON users (oidc_subject)
    WHERE oidc_subject IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS oidc_auth_states (
    state         VARCHAR(128) PRIMARY KEY,
    code_verifier VARCHAR(255) NOT NULL,
    redirect_uri  VARCHAR(512) NOT NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_oidc_auth_states_expires_at
    ON oidc_auth_states (expires_at);
