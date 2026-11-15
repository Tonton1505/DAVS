-- ============================================================================
-- V9__uc03_uc04_uc11_to_14_uc25.sql
-- Schema additions for:
--   UC03 (User Management & Ownership Transfer)
--   UC04 (Workgroup Management & Minimum SiteRole)
--   UC11 (Data Source Connections & Discovered Tables)
--   UC12 (Data Preparation Flows & Runs)
--   UC13 (Semantic Data Model)
--   UC14 (Extract Management, Refresh Schedules & Runs, Dependent Workbooks)
--   UC25 (External Platform Integrations: Slack, Salesforce)
-- ============================================================================

-- 1. UC04: Group minimum site role
ALTER TABLE groups
  ADD COLUMN IF NOT EXISTS min_role VARCHAR(30);

ALTER TABLE groups
  DROP CONSTRAINT IF EXISTS ck_groups_min_role;

ALTER TABLE groups
  ADD CONSTRAINT ck_groups_min_role
  CHECK (min_role IS NULL OR min_role IN ('SITE_ADMIN', 'CREATOR', 'EXPLORER', 'VIEWER'));

-- 2. UC11: Data sources, encrypted connections, and discovered tables
CREATE TABLE IF NOT EXISTS data_sources (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name              VARCHAR(150) NOT NULL,
  description       TEXT,
  connector_type    VARCHAR(40)  NOT NULL,
  mode              VARCHAR(20)  NOT NULL DEFAULT 'LIVE',
  status            VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
  owner_id          UUID REFERENCES users(id),
  project_id        UUID REFERENCES projects(id),
  file_bucket       VARCHAR(50),
  file_object_key   VARCHAR(500),
  file_name         VARCHAR(255),
  row_count         BIGINT       NOT NULL DEFAULT 0,
  certified         BOOLEAN      NOT NULL DEFAULT FALSE,
  quality_warning   TEXT,
  sensitivity       VARCHAR(30),
  last_refreshed_at TIMESTAMPTZ,
  created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at        TIMESTAMPTZ,
  CONSTRAINT ck_data_sources_mode CHECK (mode IN ('LIVE', 'EXTRACT')),
  CONSTRAINT ck_data_sources_status CHECK (status IN ('ACTIVE', 'REFRESHING', 'FAILED', 'PAUSED'))
);

CREATE INDEX IF NOT EXISTS ix_data_sources_owner
  ON data_sources (owner_id)
  WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS connections (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  datasource_id    UUID         NOT NULL UNIQUE REFERENCES data_sources(id) ON DELETE CASCADE,
  host             VARCHAR(255),
  port             INT,
  database_name    VARCHAR(150),
  schema_name      VARCHAR(150),
  username         VARCHAR(150),
  encrypted_secret TEXT,
  ssl_mode         VARCHAR(30),
  extra_params     JSONB        NOT NULL DEFAULT '{}'::jsonb,
  created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ds_tables (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  datasource_id    UUID         NOT NULL REFERENCES data_sources(id) ON DELETE CASCADE,
  table_name       VARCHAR(150) NOT NULL,
  schema_name      VARCHAR(150) NOT NULL DEFAULT 'public',
  row_count        BIGINT       NOT NULL DEFAULT 0,
  columns_json     JSONB        NOT NULL DEFAULT '[]'::jsonb,
  sample_rows_json JSONB        NOT NULL DEFAULT '[]'::jsonb,
  created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT ux_ds_tables_ds_name UNIQUE (datasource_id, schema_name, table_name)
);

-- 3. UC13: Semantic data model per data source
CREATE TABLE IF NOT EXISTS ds_model (
  datasource_id      UUID PRIMARY KEY REFERENCES data_sources(id) ON DELETE CASCADE,
  tables_json        JSONB       NOT NULL DEFAULT '[]'::jsonb,
  relationships_json JSONB       NOT NULL DEFAULT '[]'::jsonb,
  fields_json        JSONB       NOT NULL DEFAULT '[]'::jsonb,
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by         UUID REFERENCES users(id)
);

-- 4. UC12: Data preparation flows & runs
CREATE TABLE IF NOT EXISTS prep_flows (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  datasource_id        UUID REFERENCES data_sources(id) ON DELETE SET NULL,
  output_datasource_id UUID REFERENCES data_sources(id) ON DELETE SET NULL,
  name                 VARCHAR(150) NOT NULL,
  description          TEXT,
  steps_json           JSONB        NOT NULL DEFAULT '[]'::jsonb,
  lineage_json         JSONB        NOT NULL DEFAULT '[]'::jsonb,
  owner_id             UUID REFERENCES users(id),
  published            BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at           TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS prep_runs (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  flow_id             UUID        NOT NULL REFERENCES prep_flows(id) ON DELETE CASCADE,
  job_id              UUID REFERENCES jobs(id) ON DELETE SET NULL,
  status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  input_rows          BIGINT      NOT NULL DEFAULT 0,
  output_rows         BIGINT      NOT NULL DEFAULT 0,
  removed_duplicates  BIGINT      NOT NULL DEFAULT 0,
  replaced_nulls      BIGINT      NOT NULL DEFAULT 0,
  output_preview_json JSONB       NOT NULL DEFAULT '[]'::jsonb,
  lineage_json        JSONB       NOT NULL DEFAULT '[]'::jsonb,
  error_code          VARCHAR(30),
  error_message       TEXT,
  started_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  finished_at         TIMESTAMPTZ,
  created_by          UUID REFERENCES users(id),
  CONSTRAINT ck_prep_runs_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS ix_prep_runs_flow
  ON prep_runs (flow_id, started_at DESC);

-- 5. UC14: Extracts, refresh schedules, refresh runs & workbooks reference
CREATE TABLE IF NOT EXISTS extracts (
  id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  datasource_id      UUID        NOT NULL UNIQUE REFERENCES data_sources(id) ON DELETE CASCADE,
  storage_object_key VARCHAR(500),
  row_count          BIGINT      NOT NULL DEFAULT 0,
  snapshot_json      JSONB       NOT NULL DEFAULT '[]'::jsonb,
  version            INT         NOT NULL DEFAULT 1,
  last_refreshed_at  TIMESTAMPTZ,
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS refresh_schedules (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  datasource_id        UUID         NOT NULL UNIQUE REFERENCES data_sources(id) ON DELETE CASCADE,
  frequency            VARCHAR(30)  NOT NULL DEFAULT 'DAILY',
  run_time             VARCHAR(20)  NOT NULL DEFAULT '02:00',
  timezone             VARCHAR(64)  NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
  refresh_type         VARCHAR(20)  NOT NULL DEFAULT 'FULL',
  incremental_column   VARCHAR(150),
  alert_emails         TEXT,
  enabled              BOOLEAN      NOT NULL DEFAULT TRUE,
  paused               BOOLEAN      NOT NULL DEFAULT FALSE,
  consecutive_failures INT          NOT NULL DEFAULT 0,
  next_run_at          TIMESTAMPTZ,
  updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by           UUID REFERENCES users(id),
  CONSTRAINT ck_refresh_schedules_freq CHECK (frequency IN ('HOURLY', 'DAILY', 'WEEKLY', 'MONTHLY')),
  CONSTRAINT ck_refresh_schedules_type CHECK (refresh_type IN ('FULL', 'INCREMENTAL'))
);

CREATE TABLE IF NOT EXISTS refresh_runs (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  datasource_id  UUID        NOT NULL REFERENCES data_sources(id) ON DELETE CASCADE,
  job_id         UUID REFERENCES jobs(id) ON DELETE SET NULL,
  refresh_type   VARCHAR(20) NOT NULL DEFAULT 'FULL',
  trigger_type   VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
  status         VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  rows_processed BIGINT      NOT NULL DEFAULT 0,
  error_code     VARCHAR(30),
  error_message  TEXT,
  started_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  finished_at    TIMESTAMPTZ,
  triggered_by   UUID REFERENCES users(id),
  CONSTRAINT ck_refresh_runs_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS ix_refresh_runs_datasource
  ON refresh_runs (datasource_id, started_at DESC);

CREATE TABLE IF NOT EXISTS workbooks (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name          VARCHAR(150) NOT NULL,
  description   TEXT,
  project_id    UUID REFERENCES projects(id),
  owner_id      UUID REFERENCES users(id),
  datasource_id UUID REFERENCES data_sources(id) ON DELETE SET NULL,
  status        VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ix_workbooks_owner
  ON workbooks (owner_id)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_workbooks_datasource
  ON workbooks (datasource_id)
  WHERE deleted_at IS NULL;

-- 6. UC25: External Platform Integrations (Slack, Salesforce)
CREATE TABLE IF NOT EXISTS integrations (
  platform                VARCHAR(40) PRIMARY KEY,
  display_name            VARCHAR(100) NOT NULL,
  description             TEXT,
  status                  VARCHAR(30)  NOT NULL DEFAULT 'DISCONNECTED',
  scopes                  VARCHAR(500),
  encrypted_access_token  TEXT,
  encrypted_refresh_token TEXT,
  oauth_state             VARCHAR(128),
  account_name            VARCHAR(254),
  token_expires_at        TIMESTAMPTZ,
  connected_at            TIMESTAMPTZ,
  connected_by            UUID REFERENCES users(id),
  updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT ck_integrations_status CHECK (status IN ('CONNECTED', 'DISCONNECTED', 'RECONNECT_REQUIRED'))
);

INSERT INTO integrations (platform, display_name, description, status, scopes)
VALUES
  ('SLACK', 'Slack', 'Gửi thông báo cảnh báo làm mới và chia sẻ liên kết báo cáo qua Slack.', 'DISCONNECTED', 'chat:write,channels:read,links:write'),
  ('SALESFORCE', 'Salesforce', 'Đồng bộ dữ liệu CRM và tích hợp xác thực một lần (SSO).', 'DISCONNECTED', 'api,refresh_token,openid')
ON CONFLICT (platform) DO NOTHING;
