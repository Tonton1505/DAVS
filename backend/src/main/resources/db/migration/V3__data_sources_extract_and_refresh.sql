-- ============================================================================
-- V3__data_sources_extract_and_refresh.sql
-- Migration for UC14: extracts, refresh_schedules, refresh_runs, workbooks
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. workbooks (needed for dependent tracking in UC14 [3c])
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS workbooks (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  datasource_id UUID REFERENCES data_sources(id) ON DELETE SET NULL,
  name          VARCHAR(128) NOT NULL,
  description   VARCHAR(500),
  owner_id      UUID REFERENCES users(id),
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ix_workbooks_datasource ON workbooks (datasource_id) WHERE deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 2. extracts (persisted extract metadata)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS extracts (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  datasource_id UUID NOT NULL REFERENCES data_sources(id) ON DELETE CASCADE,
  storage_path  VARCHAR(500),
  row_count     BIGINT      NOT NULL DEFAULT 0,
  size_bytes    BIGINT      NOT NULL DEFAULT 0,
  status        VARCHAR(32) NOT NULL DEFAULT 'READY',
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_extracts_datasource ON extracts (datasource_id);

-- ----------------------------------------------------------------------------
-- 3. refresh_schedules (UC14 schedule configuration & consecutive failure counter)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS refresh_schedules (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  datasource_id        UUID NOT NULL REFERENCES data_sources(id) ON DELETE CASCADE,
  frequency            VARCHAR(32) NOT NULL DEFAULT 'DAILY',
  run_time             VARCHAR(8)  NOT NULL DEFAULT '02:00',
  time_zone            VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
  refresh_type         VARCHAR(32) NOT NULL DEFAULT 'FULL',
  alert_emails         JSONB       NOT NULL DEFAULT '[]'::jsonb,
  status               VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  consecutive_failures INT         NOT NULL DEFAULT 0,
  last_run_at          TIMESTAMPTZ,
  next_run_at          TIMESTAMPTZ,
  created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_refresh_schedules_datasource UNIQUE (datasource_id),
  CONSTRAINT ck_refresh_schedules_status CHECK (status IN ('ACTIVE', 'PAUSED', 'DISABLED'))
);

CREATE INDEX IF NOT EXISTS ix_refresh_schedules_next_run ON refresh_schedules (next_run_at ASC) WHERE status = 'ACTIVE';

-- ----------------------------------------------------------------------------
-- 4. refresh_runs (execution history of extract refreshes)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS refresh_runs (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  datasource_id  UUID NOT NULL REFERENCES data_sources(id) ON DELETE CASCADE,
  schedule_id    UUID REFERENCES refresh_schedules(id) ON DELETE SET NULL,
  refresh_type   VARCHAR(32) NOT NULL DEFAULT 'FULL',
  status         VARCHAR(32) NOT NULL DEFAULT 'RUNNING',
  rows_processed BIGINT      NOT NULL DEFAULT 0,
  error_message  TEXT,
  started_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  finished_at    TIMESTAMPTZ,
  CONSTRAINT ck_refresh_runs_status CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS ix_refresh_runs_datasource ON refresh_runs (datasource_id, started_at DESC);
