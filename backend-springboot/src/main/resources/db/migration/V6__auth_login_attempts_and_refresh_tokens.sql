-- ============================================================================
-- V6__auth_login_attempts_and_refresh_tokens.sql
-- Login lockout tracking and hashed refresh token persistence (UC01)
-- ============================================================================

CREATE TABLE IF NOT EXISTS login_attempts (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email          VARCHAR(254) NOT NULL,
  ip_address     VARCHAR(64)  NOT NULL,
  failed_count   INT          NOT NULL DEFAULT 0,
  locked_until   TIMESTAMPTZ,
  last_failed_at TIMESTAMPTZ,
  updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT ux_login_attempts_email_ip UNIQUE (email, ip_address)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  VARCHAR(64) NOT NULL UNIQUE,
  remember_me BOOLEAN     NOT NULL DEFAULT FALSE,
  expires_at  TIMESTAMPTZ NOT NULL,
  revoked_at  TIMESTAMPTZ,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_refresh_tokens_user
  ON refresh_tokens (user_id);
