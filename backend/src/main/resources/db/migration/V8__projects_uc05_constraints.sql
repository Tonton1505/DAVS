-- ============================================================================
-- V8__projects_uc05_constraints.sql
-- Support INHERIT/CUSTOM/LOCKED permission modes and child sibling uniqueness (UC05)
-- ============================================================================

ALTER TABLE projects
  DROP CONSTRAINT IF EXISTS ck_projects_permission_mode;

ALTER TABLE projects
  ADD CONSTRAINT ck_projects_permission_mode
  CHECK (permission_mode IN ('INHERIT', 'CUSTOM', 'CUSTOMIZABLE', 'LOCKED'));

CREATE UNIQUE INDEX IF NOT EXISTS ux_projects_parent_name_ci
  ON projects (parent_id, lower(name))
  WHERE parent_id IS NOT NULL AND deleted_at IS NULL;
