-- UC25..UC26: Bảng external_integrations, connected_apps
CREATE TABLE IF NOT EXISTS connected_apps (id BIGSERIAL PRIMARY KEY, app_name VARCHAR(100) UNIQUE NOT NULL, trust_mode VARCHAR(32) NOT NULL, client_id VARCHAR(128) NOT NULL, secret_hash VARCHAR(255) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
