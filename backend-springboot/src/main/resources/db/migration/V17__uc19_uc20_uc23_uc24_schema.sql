-- =====================================================================
-- V17: Schema for Member 2 Batch 3:
--      UC19 (Dashboard Viewing & Interaction),
--      UC20 (Ad-hoc Exploration & Custom Views),
--      UC23 (Data & Report Export),
--      UC24 (Comments & Threaded Replies)
-- =====================================================================

-- 1. Dashboards (UC19 & UC17/UC18)
CREATE TABLE IF NOT EXISTS dashboards (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workbook_id UUID         NOT NULL REFERENCES workbooks (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    size_type   VARCHAR(32)  NOT NULL DEFAULT 'FIXED',
    width       INT          DEFAULT 1366,
    height      INT          DEFAULT 768,
    tiles       JSONB        NOT NULL DEFAULT '[]'::jsonb,
    actions     JSONB        NOT NULL DEFAULT '[]'::jsonb,
    definition  JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_by  UUID         REFERENCES users (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_dashboards_workbook ON dashboards (workbook_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_dashboards_created_by ON dashboards (created_by) WHERE deleted_at IS NULL;

-- 2. Custom Views (UC20)
CREATE TABLE IF NOT EXISTS custom_views (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dashboard_id UUID         NOT NULL REFERENCES dashboards (id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    filters      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    parameters   JSONB        NOT NULL DEFAULT '{}'::jsonb,
    is_default   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_shared    BOOLEAN      NOT NULL DEFAULT FALSE,
    owner_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_custom_views_dashboard ON custom_views (dashboard_id);
CREATE INDEX IF NOT EXISTS idx_custom_views_owner ON custom_views (owner_id);

-- 3. Comments & Threaded Replies (UC24)
CREATE TABLE IF NOT EXISTS comments (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_type VARCHAR(32)  NOT NULL,
    resource_id   UUID         NOT NULL,
    parent_id     UUID         REFERENCES comments (id) ON DELETE CASCADE,
    author_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    text          VARCHAR(1000) NOT NULL,
    mentions      JSONB        NOT NULL DEFAULT '[]'::jsonb,
    snapshot_url  VARCHAR(512),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_comments_resource ON comments (resource_type, resource_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_comments_parent ON comments (parent_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_comments_author ON comments (author_id) WHERE deleted_at IS NULL;

-- 4. Seed a standard Demo Workbook & Dashboard for instant exploration if not present
INSERT INTO workbooks (id, project_id, name, description, current_version, owner_id, definition, import_status, created_at)
SELECT
    '00000000-0000-0000-0000-000000000401',
    p.id,
    'Báo cáo bán hàng Q4',
    'Dashboard phân tích doanh số và hiệu suất sản phẩm quý 4',
    1,
    u.id,
    '{"sheets": ["Tổng quan kinh doanh", "Phân tích sản phẩm"], "theme": "default"}'::jsonb,
    'IMPORTED',
    now()
FROM projects p
JOIN users u ON u.site_role = 'SITE_ADMIN'
WHERE p.is_default = TRUE
LIMIT 1
ON CONFLICT DO NOTHING;

INSERT INTO dashboards (id, workbook_id, name, description, definition, created_by, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000501',
    '00000000-0000-0000-0000-000000000401',
    'Tổng quan kinh doanh',
    'Dashboard tương tác phân tích doanh thu khu vực và sản phẩm nổi bật',
    '{
        "title": "Tổng quan kinh doanh",
        "eyebrow": "Trang chủ › Báo cáo bán hàng Q4",
        "sheets": [
            {"id": "sheet-1", "name": "Doanh số theo tháng", "type": "bar"},
            {"id": "sheet-2", "name": "Cơ cấu doanh thu", "type": "pie"}
        ],
        "filters": [
            {"field": "region", "label": "Vùng miền", "options": ["Tất cả vùng", "Miền Bắc", "Miền Trung", "Miền Nam"], "defaultValue": "Tất cả vùng"}
        ],
        "parameters": [
            {"field": "topProducts", "label": "Top sản phẩm", "type": "integer", "defaultValue": 10, "min": 3, "max": 20}
        ]
    }'::jsonb,
    NULL,
    now()
)
ON CONFLICT (id) DO NOTHING;
