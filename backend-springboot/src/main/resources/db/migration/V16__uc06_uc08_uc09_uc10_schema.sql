-- =====================================================================
-- V9: Schema for UC06 (Collections), UC08 & UC10 (Workbooks & Versions),
--     and UC09 (Data Labels & Categories)
-- =====================================================================

-- 1. Personal Collections (UC06 [6d])
CREATE TABLE IF NOT EXISTS collections (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(500),
    owner_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_collections_owner ON collections (owner_id);

CREATE TABLE IF NOT EXISTS collection_items (
    collection_id UUID        NOT NULL REFERENCES collections (id) ON DELETE CASCADE,
    resource_type VARCHAR(32) NOT NULL,
    resource_id   UUID        NOT NULL,
    added_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (collection_id, resource_type, resource_id)
);

-- 2. Workbooks & Workbook Versions (UC08 & UC10)
CREATE TABLE IF NOT EXISTS workbooks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    description     VARCHAR(500),
    current_version INT          NOT NULL DEFAULT 1,
    owner_id        UUID         NOT NULL REFERENCES users (id),
    definition      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    import_status   VARCHAR(32)  NOT NULL DEFAULT 'IMPORTED',
    needs_reconnect BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_workbooks_project_name_active
    ON workbooks (project_id, lower(name))
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_workbooks_project_active
    ON workbooks (project_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_workbooks_owner
    ON workbooks (owner_id)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS workbook_versions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workbook_id    UUID         NOT NULL REFERENCES workbooks (id) ON DELETE CASCADE,
    version_number INT          NOT NULL,
    object_key     VARCHAR(512) NOT NULL,
    file_name      VARCHAR(255) NOT NULL,
    size_bytes     BIGINT       NOT NULL DEFAULT 0,
    note           VARCHAR(500),
    created_by     UUID         REFERENCES users (id) ON DELETE SET NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ,
    purged         BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_workbook_versions_number
    ON workbook_versions (workbook_id, version_number);

CREATE INDEX IF NOT EXISTS idx_workbook_versions_workbook
    ON workbook_versions (workbook_id, version_number DESC);

-- 3. Data Labels & Categories (UC09)
CREATE TABLE IF NOT EXISTS label_categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(250),
    is_warning  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by  UUID         REFERENCES users (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_label_categories_name_lower
    ON label_categories (lower(name));

CREATE TABLE IF NOT EXISTS labels (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID         NOT NULL REFERENCES label_categories (id) ON DELETE CASCADE,
    value       VARCHAR(50)  NOT NULL,
    description VARCHAR(250),
    is_warning  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by  UUID         REFERENCES users (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_labels_category_value_lower
    ON labels (category_id, lower(value));

CREATE TABLE IF NOT EXISTS label_assignments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    label_id    UUID        NOT NULL REFERENCES labels (id) ON DELETE CASCADE,
    target_type VARCHAR(32) NOT NULL,
    target_id   UUID        NOT NULL,
    assigned_by UUID        REFERENCES users (id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_label_assignments_target
    ON label_assignments (label_id, target_type, target_id);

CREATE INDEX IF NOT EXISTS idx_label_assignments_target_lookup
    ON label_assignments (target_type, target_id);

-- Seed default label categories (UC09)
INSERT INTO label_categories (id, name, description, is_warning, created_at)
VALUES
    ('00000000-0000-0000-0000-000000000301', 'Độ nhạy cảm', 'Phân loại mức độ nhạy cảm và bảo mật dữ liệu', FALSE, now()),
    ('00000000-0000-0000-0000-000000000302', 'Chứng nhận', 'Chứng nhận nguồn dữ liệu và báo cáo chính thức', FALSE, now()),
    ('00000000-0000-0000-0000-000000000303', 'Cảnh báo chất lượng', 'Cảnh báo chất lượng dữ liệu hiển thị nổi bật cho người xem', TRUE, now())
ON CONFLICT (lower(name)) DO NOTHING;
