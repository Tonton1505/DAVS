-- UC11..UC14: Bảng data_sources, extract_refresh_schedules, data_prep_flows, data_model_relations
CREATE TABLE IF NOT EXISTS data_sources (id BIGSERIAL PRIMARY KEY, name VARCHAR(128) NOT NULL, source_type VARCHAR(32) NOT NULL, connection_mode VARCHAR(16) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
