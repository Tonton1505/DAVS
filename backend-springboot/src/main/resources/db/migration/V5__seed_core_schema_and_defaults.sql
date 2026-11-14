-- ============================================================================
-- V5__seed_core_schema_and_defaults.sql
-- Creates groups, group_members, projects, license_pools and seeds defaults (T-011)
-- ============================================================================

-- 1. groups & group_members
CREATE TABLE IF NOT EXISTS groups (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name        VARCHAR(150) NOT NULL,
  description TEXT,
  is_system   BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID REFERENCES users(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_groups_name_ci
  ON groups (lower(name));

CREATE TABLE IF NOT EXISTS group_members (
  group_id   UUID        NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
  user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (group_id, user_id)
);

CREATE INDEX IF NOT EXISTS ix_group_members_user
  ON group_members (user_id);

-- 2. projects
CREATE TABLE IF NOT EXISTS projects (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name            VARCHAR(150) NOT NULL,
  description     TEXT,
  parent_id       UUID REFERENCES projects(id),
  owner_id        UUID REFERENCES users(id),
  permission_mode VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMIZABLE',
  is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ,
  CONSTRAINT ck_projects_permission_mode CHECK (permission_mode IN ('CUSTOMIZABLE', 'LOCKED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_projects_root_name_ci
  ON projects (lower(name))
  WHERE parent_id IS NULL AND deleted_at IS NULL;

-- 3. license_pools
CREATE TABLE IF NOT EXISTS license_pools (
  role       VARCHAR(30) PRIMARY KEY,
  total      INT         NOT NULL DEFAULT 0,
  used       INT         NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_license_pools_role CHECK (role IN ('SITE_ADMIN', 'CREATOR', 'EXPLORER', 'VIEWER')),
  CONSTRAINT ck_license_pools_counts CHECK (total >= 0 AND used >= 0 AND used <= total)
);

-- 4. Idempotent default seed data (never overwrites existing rows)
INSERT INTO groups (name, description, is_system)
SELECT 'All Users', 'Default system group containing all active users', TRUE
WHERE NOT EXISTS (
  SELECT 1 FROM groups WHERE lower(name) = lower('All Users')
);

INSERT INTO projects (name, description, permission_mode, is_default)
SELECT 'Default', 'Default root project workspace', 'CUSTOMIZABLE', TRUE
WHERE NOT EXISTS (
  SELECT 1 FROM projects WHERE parent_id IS NULL AND deleted_at IS NULL AND lower(name) = lower('Default')
);

INSERT INTO license_pools (role, total, used) VALUES
  ('SITE_ADMIN', 5, 0),
  ('CREATOR', 10, 0),
  ('EXPLORER', 30, 0),
  ('VIEWER', 200, 0)
ON CONFLICT (role) DO NOTHING;
