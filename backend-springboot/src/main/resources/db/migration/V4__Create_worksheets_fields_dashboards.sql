-- UC15..UC20: Bảng worksheets, calculated_fields, parameters, dashboards, dashboard_actions, custom_views
CREATE TABLE IF NOT EXISTS dashboards (id BIGSERIAL PRIMARY KEY, name VARCHAR(128) NOT NULL, size_mode VARCHAR(32) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
