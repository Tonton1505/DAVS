-- ============================================================================
-- V1__baseline.sql
-- Baseline schema for DAVS (Data Analytics & Visualization System)
-- Extensions: pgcrypto
-- Core baseline tables: users, system_settings, audit_logs, jobs
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------------------------------------------------------
-- 1. users (basic core user table per context.md section 4)
-- ----------------------------------------------------------------------------
CREATE TABLE users (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email                VARCHAR(254) NOT NULL,
  password_hash        VARCHAR(255) NOT NULL,
  full_name            VARCHAR(150),
  auth_method          VARCHAR(30)  NOT NULL DEFAULT 'LOCAL',
  site_role            VARCHAR(30)  NOT NULL DEFAULT 'VIEWER',
  status               VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
  must_change_password BOOLEAN      NOT NULL DEFAULT FALSE,
  last_login_at        TIMESTAMPTZ,
  last_activity_at     TIMESTAMPTZ,
  terms_accepted_at    TIMESTAMPTZ,
  created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by           UUID REFERENCES users(id),
  deleted_at           TIMESTAMPTZ,
  CONSTRAINT ck_users_site_role CHECK (site_role IN ('SITE_ADMIN', 'CREATOR', 'EXPLORER', 'VIEWER')),
  CONSTRAINT ck_users_status CHECK (status IN ('PENDING', 'UNINVITED', 'ACTIVE', 'DISABLED', 'LOCKED'))
);

CREATE UNIQUE INDEX ux_users_email_ci
  ON users (lower(email))
  WHERE deleted_at IS NULL;

CREATE INDEX ix_users_site_role_status
  ON users (site_role, status)
  WHERE deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 2. system_settings (configurable system limits per context.md section 4 & 8)
-- ----------------------------------------------------------------------------
CREATE TABLE system_settings (
  setting_key   VARCHAR(100) PRIMARY KEY,
  setting_value TEXT         NOT NULL,
  value_type    VARCHAR(30)  NOT NULL DEFAULT 'STRING',
  description   VARCHAR(500),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by    UUID REFERENCES users(id)
);

-- ----------------------------------------------------------------------------
-- 3. audit_logs (append-only security & compliance audit trail per D10)
-- ----------------------------------------------------------------------------
CREATE TABLE audit_logs (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  action        VARCHAR(100) NOT NULL,
  actor_id      UUID REFERENCES users(id),
  actor_email   VARCHAR(254),
  resource_type VARCHAR(50),
  resource_id   VARCHAR(100),
  before_state  JSONB,
  after_state   JSONB,
  ip_address    VARCHAR(64),
  user_agent    VARCHAR(500),
  trace_id      VARCHAR(64),
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX ix_audit_logs_actor_id ON audit_logs (actor_id, created_at DESC);
CREATE INDEX ix_audit_logs_action ON audit_logs (action, created_at DESC);
CREATE INDEX ix_audit_logs_resource ON audit_logs (resource_type, resource_id);

-- ----------------------------------------------------------------------------
-- 4. jobs (background queue backed by PostgreSQL FOR UPDATE SKIP LOCKED)
-- ----------------------------------------------------------------------------
CREATE TABLE jobs (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  type         VARCHAR(50)  NOT NULL,
  payload      JSONB        NOT NULL DEFAULT '{}'::jsonb,
  status       VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
  run_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
  attempts     INT          NOT NULL DEFAULT 0,
  max_attempts INT          NOT NULL DEFAULT 3,
  last_error   TEXT,
  locked_at    TIMESTAMPTZ,
  locked_by    VARCHAR(100),
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by   UUID REFERENCES users(id),
  CONSTRAINT ck_jobs_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX ix_jobs_poll
  ON jobs (run_at ASC)
  WHERE status = 'PENDING';
