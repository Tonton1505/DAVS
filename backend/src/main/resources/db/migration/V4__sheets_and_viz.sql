-- ============================================================================
-- V4__sheets_and_viz.sql
-- Migration for UC15: sheets table with unique name per workbook (BUS-VIZ-04)
-- ============================================================================

CREATE TABLE IF NOT EXISTS sheets (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workbook_id UUID NOT NULL REFERENCES workbooks(id) ON DELETE CASCADE,
  name        VARCHAR(128) NOT NULL,
  definition  JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_sheets_workbook_name UNIQUE (workbook_id, name)
);

CREATE INDEX IF NOT EXISTS ix_sheets_workbook ON sheets (workbook_id);
