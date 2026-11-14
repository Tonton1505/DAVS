-- ============================================================================
-- V2__permission_rules.sql
-- Permission rules table for RBAC + resource-level ALLOW/DENY policies (T-006)
-- ============================================================================

CREATE TABLE permission_rules (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  subject_type  VARCHAR(20) NOT NULL,
  subject_id    UUID        NOT NULL,
  resource_type VARCHAR(30) NOT NULL,
  resource_id   UUID        NOT NULL,
  capability    VARCHAR(40) NOT NULL,
  effect        VARCHAR(10) NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by    UUID REFERENCES users(id),
  CONSTRAINT ck_permission_rules_subject_type CHECK (subject_type IN ('USER', 'GROUP')),
  CONSTRAINT ck_permission_rules_resource_type CHECK (resource_type IN ('PROJECT', 'WORKBOOK', 'DATASOURCE', 'DASHBOARD')),
  CONSTRAINT ck_permission_rules_capability CHECK (capability IN (
    'VIEW', 'INTERACT', 'FILTER', 'DOWNLOAD', 'COMMENT',
    'WEB_EDIT', 'SAVE', 'SHARE', 'DELETE', 'CHANGE_PERMISSIONS', 'PUBLISH', 'CONNECT'
  )),
  CONSTRAINT ck_permission_rules_effect CHECK (effect IN ('ALLOW', 'DENY')),
  CONSTRAINT ux_permission_rules_unique UNIQUE (subject_type, subject_id, resource_type, resource_id, capability)
);

CREATE INDEX ix_permission_rules_resource
  ON permission_rules (resource_type, resource_id);

CREATE INDEX ix_permission_rules_subject
  ON permission_rules (subject_type, subject_id);
