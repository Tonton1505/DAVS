-- ============================================================================
-- V9__viz_and_analytics_studio.sql
-- Supports UC15 (Sheets & Query), UC16 (Calculated Fields & Parameters),
-- UC17 (Dashboards), and UC18 (Dashboard Actions & Tooltips)
-- ============================================================================

-- Data sources (for UC15 / UC16 modeling & calculation reference)
CREATE TABLE IF NOT EXISTS data_sources (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id        UUID REFERENCES projects(id) ON DELETE SET NULL,
    name              VARCHAR(150) NOT NULL,
    type              VARCHAR(50)  NOT NULL DEFAULT 'POSTGRESQL',
    mode              VARCHAR(20)  NOT NULL DEFAULT 'EXTRACT',
    connection_config JSONB,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Workbooks (for UC15 / UC17 sheet & dashboard containers)
CREATE TABLE IF NOT EXISTS workbooks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID REFERENCES projects(id) ON DELETE SET NULL,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    owner_id    UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- UC15: sheets
CREATE TABLE IF NOT EXISTS sheets (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workbook_id    UUID         NOT NULL,
    name           VARCHAR(128) NOT NULL,
    chart_type     VARCHAR(32)  NOT NULL DEFAULT 'BAR',
    definition     JSONB        NOT NULL DEFAULT '{}',
    tooltip_config JSONB,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_sheets_wb_name UNIQUE (workbook_id, name)
);

CREATE INDEX IF NOT EXISTS ix_sheets_workbook ON sheets(workbook_id);

-- UC16: calculated_fields
CREATE TABLE IF NOT EXISTS calculated_fields (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    datasource_id UUID         NOT NULL,
    name          VARCHAR(128) NOT NULL,
    formula       TEXT         NOT NULL,
    return_type   VARCHAR(32)  NOT NULL DEFAULT 'STRING',
    role          VARCHAR(32)  NOT NULL DEFAULT 'DIMENSION',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_calc_fields_ds_name UNIQUE (datasource_id, name)
);

CREATE INDEX IF NOT EXISTS ix_calc_fields_ds ON calculated_fields(datasource_id);

-- UC16: parameters
CREATE TABLE IF NOT EXISTS parameters (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workbook_id           UUID         NOT NULL,
    name                  VARCHAR(128) NOT NULL,
    data_type             VARCHAR(32)  NOT NULL DEFAULT 'INTEGER',
    allowable_values_type VARCHAR(32)  NOT NULL DEFAULT 'ALL',
    list_values           JSONB,
    range_min             NUMERIC,
    range_max             NUMERIC,
    range_step            NUMERIC,
    current_value         TEXT         NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_parameters_wb_name UNIQUE (workbook_id, name)
);

CREATE INDEX IF NOT EXISTS ix_parameters_wb ON parameters(workbook_id);

-- UC17 & UC18: dashboards
CREATE TABLE IF NOT EXISTS dashboards (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workbook_id UUID         NOT NULL,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(500),
    size_type   VARCHAR(32)  NOT NULL DEFAULT 'FIXED',
    width       INT          DEFAULT 1366,
    height      INT          DEFAULT 768,
    tiles       JSONB        NOT NULL DEFAULT '[]',
    actions     JSONB        NOT NULL DEFAULT '[]',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_dashboards_wb_name UNIQUE (workbook_id, name)
);

CREATE INDEX IF NOT EXISTS ix_dashboards_wb ON dashboards(workbook_id);
