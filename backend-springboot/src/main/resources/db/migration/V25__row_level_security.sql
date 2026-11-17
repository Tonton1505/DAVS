-- ============================================================================
-- V25__row_level_security.sql
-- T-101: Row-Level Security (RLS) mappings and per-datasource toggle
-- ============================================================================

ALTER TABLE data_sources
    ADD COLUMN IF NOT EXISTS rls_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS user_row_filters (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    datasource_id  UUID         NOT NULL REFERENCES data_sources(id) ON DELETE CASCADE,
    subject_type   VARCHAR(20)  NOT NULL,
    subject_id     UUID         NOT NULL,
    field_name     VARCHAR(150) NOT NULL,
    allowed_values JSONB        NOT NULL DEFAULT '[]'::jsonb,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_user_row_filters_subject CHECK (subject_type IN ('USER', 'GROUP'))
);

CREATE INDEX IF NOT EXISTS ix_user_row_filters_lookup
    ON user_row_filters (datasource_id, subject_type, subject_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_user_row_filters_subject_field
    ON user_row_filters (datasource_id, subject_type, subject_id, lower(field_name));
