-- ============================================================================
-- V2__datasource_and_model.sql
-- Migration for UC11, UC13, UC14: data_sources and ds_model
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. data_sources
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS data_sources (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name              VARCHAR(128) NOT NULL,
  source_type       VARCHAR(32)  NOT NULL DEFAULT 'POSTGRESQL',
  connection_mode   VARCHAR(16)  NOT NULL DEFAULT 'EXTRACT',
  connection_config JSONB        NOT NULL DEFAULT '{}'::jsonb,
  status            VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
  owner_id          UUID REFERENCES users(id),
  created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at        TIMESTAMPTZ,
  CONSTRAINT ck_data_sources_mode CHECK (connection_mode IN ('LIVE', 'EXTRACT')),
  CONSTRAINT ck_data_sources_status CHECK (status IN ('ACTIVE', 'REFRESHING', 'ERROR', 'DISABLED'))
);

CREATE INDEX IF NOT EXISTS ix_data_sources_owner ON data_sources (owner_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_data_sources_status ON data_sources (status) WHERE deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 2. ds_model (logical / physical data model for UC13)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ds_model (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  datasource_id  UUID NOT NULL REFERENCES data_sources(id) ON DELETE CASCADE,
  tables         JSONB NOT NULL DEFAULT '[]'::jsonb,
  relationships  JSONB NOT NULL DEFAULT '[]'::jsonb,
  fields         JSONB NOT NULL DEFAULT '[]'::jsonb,
  version        INT   NOT NULL DEFAULT 1,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_ds_model_datasource UNIQUE (datasource_id)
);

CREATE INDEX IF NOT EXISTS ix_ds_model_datasource ON ds_model (datasource_id);
