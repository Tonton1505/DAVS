-- V24__publish_share_connected_apps.sql
-- UC21 Publish Workbook, UC22 Content Sharing, UC26 Connected Apps

-- 1. UC21: Published Workbooks Records
CREATE TABLE IF NOT EXISTS published_workbooks (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workbook_id             UUID NOT NULL REFERENCES workbooks (id) ON DELETE CASCADE,
    project_id              UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    publisher_id            UUID REFERENCES users (id) ON DELETE SET NULL,
    destination             VARCHAR(32) NOT NULL DEFAULT 'INTERNAL_SERVER',
    published_name          VARCHAR(255) NOT NULL,
    description             VARCHAR(500),
    visible_sheets          JSONB NOT NULL DEFAULT '[]'::jsonb,
    data_source_mode        VARCHAR(32) NOT NULL DEFAULT 'EMBEDDED',
    is_packaged_twbx        BOOLEAN NOT NULL DEFAULT FALSE,
    has_embedded_credentials BOOLEAN NOT NULL DEFAULT TRUE,
    public_url              VARCHAR(500),
    version                 INT NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_published_workbooks_workbook
    ON published_workbooks (workbook_id);

CREATE INDEX IF NOT EXISTS idx_published_workbooks_project
    ON published_workbooks (project_id);

-- 2. UC22: Content Sharing Records
CREATE TABLE IF NOT EXISTS content_shares (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_type           VARCHAR(32) NOT NULL,
    resource_id             UUID NOT NULL,
    shared_by               UUID REFERENCES users (id) ON DELETE SET NULL,
    recipient_type          VARCHAR(32) NOT NULL,
    recipient_id            UUID REFERENCES users (id) ON DELETE CASCADE,
    recipient_identifier    VARCHAR(255) NOT NULL,
    permission_level        VARCHAR(32) NOT NULL DEFAULT 'VIEW',
    message                 VARCHAR(500),
    send_email_notification BOOLEAN NOT NULL DEFAULT TRUE,
    share_link              VARCHAR(500),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_content_shares_resource
    ON content_shares (resource_type, resource_id);

CREATE INDEX IF NOT EXISTS idx_content_shares_recipient
    ON content_shares (recipient_identifier);

-- 3. UC26: Connected Applications
CREATE TABLE IF NOT EXISTS connected_apps (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(255) NOT NULL,
    trust_type              VARCHAR(32) NOT NULL DEFAULT 'DIRECT_TRUST',
    status                  VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    client_id               VARCHAR(100) NOT NULL UNIQUE,
    client_secret_hash      VARCHAR(255) NOT NULL,
    allowed_domains         JSONB NOT NULL DEFAULT '[]'::jsonb,
    app_url                 VARCHAR(500),
    permission_scope        VARCHAR(32) NOT NULL DEFAULT 'ALL',
    project_id              UUID REFERENCES projects (id) ON DELETE SET NULL,
    run_as_user_id          UUID REFERENCES users (id) ON DELETE SET NULL,
    created_by              UUID REFERENCES users (id) ON DELETE SET NULL,
    secret_rotated_at       TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_connected_apps_name_active
    ON connected_apps (lower(name));

CREATE INDEX IF NOT EXISTS idx_connected_apps_status
    ON connected_apps (status);

-- 4. UC26: Connected App Issued Tokens
CREATE TABLE IF NOT EXISTS connected_app_tokens (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_id                  UUID NOT NULL REFERENCES connected_apps (id) ON DELETE CASCADE,
    token_hash              VARCHAR(255) NOT NULL UNIQUE,
    user_id                 UUID REFERENCES users (id) ON DELETE SET NULL,
    domain                  VARCHAR(255),
    expires_at              TIMESTAMPTZ NOT NULL,
    revoked                 BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_connected_app_tokens_app
    ON connected_app_tokens (app_id, revoked);

CREATE INDEX IF NOT EXISTS idx_connected_app_tokens_hash
    ON connected_app_tokens (token_hash);
