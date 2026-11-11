-- ============================================================================
-- V2__seed_initial_data.sql
-- T-011: Core tables for groups, projects, license_pools and idempotent seed data
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. license_pools (tracks total and used licenses per SiteRole)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS license_pools (
  role       VARCHAR(30) PRIMARY KEY,
  total      INT         NOT NULL CHECK (total >= 0),
  used       INT         NOT NULL DEFAULT 0 CHECK (used >= 0),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_license_pools_role CHECK (role IN ('SITE_ADMIN', 'CREATOR', 'EXPLORER', 'VIEWER'))
);

-- ----------------------------------------------------------------------------
-- 2. groups & group_members (UC04 & system group "All Users")
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS groups (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name        VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  min_role    VARCHAR(30)  NOT NULL DEFAULT 'VIEWER',
  is_system   BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID REFERENCES users(id),
  deleted_at  TIMESTAMPTZ,
  CONSTRAINT ck_groups_min_role CHECK (min_role IN ('SITE_ADMIN', 'CREATOR', 'EXPLORER', 'VIEWER'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_groups_name_ci
  ON groups (lower(name))
  WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS group_members (
  group_id UUID        NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
  user_id  UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  added_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  added_by UUID REFERENCES users(id),
  PRIMARY KEY (group_id, user_id)
);

-- ----------------------------------------------------------------------------
-- 3. projects (UC05 & default project "Default")
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS projects (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  parent_id       UUID REFERENCES projects(id),
  name            VARCHAR(128) NOT NULL,
  description     VARCHAR(500),
  owner_id        UUID REFERENCES users(id),
  permission_mode VARCHAR(30)  NOT NULL DEFAULT 'INHERIT',
  is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by      UUID REFERENCES users(id),
  deleted_at      TIMESTAMPTZ,
  CONSTRAINT ck_projects_permission_mode CHECK (permission_mode IN ('INHERIT', 'CUSTOM', 'LOCKED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_projects_parent_name_ci
  ON projects (COALESCE(parent_id, '00000000-0000-0000-0000-000000000000'::uuid), lower(name))
  WHERE deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 4. Default system_settings (idempotent: never overwrites existing values)
-- ----------------------------------------------------------------------------
INSERT INTO system_settings (setting_key, setting_value, value_type, description)
VALUES
  ('session.idleMinutes', '30', 'INTEGER', 'Idle session timeout in minutes'),
  ('login.maxFailures', '5', 'INTEGER', 'Consecutive failed login attempts before lock'),
  ('login.lockMinutes', '15', 'INTEGER', 'Account lock duration in minutes'),
  ('upload.maxBytes', '1073741824', 'LONG', 'Maximum upload size in bytes (default 1 GB)'),
  ('workbook.maxVersions', '25', 'INTEGER', 'Maximum stored versions per workbook'),
  ('export.maxRows', '500000', 'INTEGER', 'Maximum rows allowed in data export'),
  ('export.maxBytes', '104857600', 'LONG', 'Maximum export file size in bytes (default 100 MB)'),
  ('chart.maxMarks', '100000', 'INTEGER', 'Maximum chart marks before warning ERR-VIZ-04'),
  ('trash.retentionDays', '30', 'INTEGER', 'Days soft-deleted items remain in trash'),
  ('audit.retentionMonths', '12', 'INTEGER', 'Minimum retention period for audit logs in months'),
  ('import.maxUsersPerFile', '1000', 'INTEGER', 'Maximum user rows per CSV import file'),
  ('refresh.maxConcurrent', '4', 'INTEGER', 'Maximum concurrent extract refresh jobs per site'),
  ('refresh.pauseAfterFailures', '5', 'INTEGER', 'Consecutive failures before pausing schedule'),
  ('comment.editWindowHours', '24', 'INTEGER', 'Hours allowed for editing own comment'),
  ('share.maxRecipients', '100', 'INTEGER', 'Maximum recipients per share action'),
  ('project.maxDepth', '5', 'INTEGER', 'Maximum nested folder depth for projects'),
  ('publish.publicEnabled', 'false', 'BOOLEAN', 'Whether public publishing outside organization is allowed')
ON CONFLICT (setting_key) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 5. Default license_pools (idempotent: never overwrites existing pools)
-- ----------------------------------------------------------------------------
INSERT INTO license_pools (role, total, used)
VALUES
  ('SITE_ADMIN', 5, 0),
  ('CREATOR', 20, 0),
  ('EXPLORER', 50, 0),
  ('VIEWER', 200, 0)
ON CONFLICT (role) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 6. Default system group "All Users" (idempotent)
-- ----------------------------------------------------------------------------
INSERT INTO groups (id, name, description, min_role, is_system)
SELECT '00000000-0000-0000-0000-000000000001'::uuid,
       'All Users',
       'Nhóm hệ thống mặc định chứa mọi người dùng trong tổ chức',
       'VIEWER',
       TRUE
WHERE NOT EXISTS (
  SELECT 1 FROM groups WHERE lower(name) = 'all users' AND deleted_at IS NULL
);

-- ----------------------------------------------------------------------------
-- 7. Default root project "Default" (idempotent)
-- ----------------------------------------------------------------------------
INSERT INTO projects (id, parent_id, name, description, permission_mode, is_default)
SELECT '00000000-0000-0000-0000-000000000002'::uuid,
       NULL,
       'Default',
       'Dự án mặc định của hệ thống DAVS',
       'INHERIT',
       TRUE
WHERE NOT EXISTS (
  SELECT 1 FROM projects WHERE parent_id IS NULL AND lower(name) = 'default' AND deleted_at IS NULL
);
