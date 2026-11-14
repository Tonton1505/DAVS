-- ============================================================================
-- V7__user_invitations_and_import_reports.sql
-- User invitations (7-day one-time token) and CSV import job tracking (UC02)
-- ============================================================================

CREATE TABLE IF NOT EXISTS invitations (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  VARCHAR(64) NOT NULL UNIQUE,
  expires_at  TIMESTAMPTZ NOT NULL,
  accepted_at TIMESTAMPTZ,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by  UUID REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS ix_invitations_user
  ON invitations (user_id);

CREATE TABLE IF NOT EXISTS user_import_jobs (
  job_id        UUID PRIMARY KEY REFERENCES jobs(id) ON DELETE CASCADE,
  auth_method   VARCHAR(30) NOT NULL,
  status        VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  total_rows    INT         NOT NULL DEFAULT 0,
  created_count INT         NOT NULL DEFAULT 0,
  skipped_count INT         NOT NULL DEFAULT 0,
  error_count   INT         NOT NULL DEFAULT 0,
  error_csv     TEXT,
  created_by    UUID REFERENCES users(id),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
