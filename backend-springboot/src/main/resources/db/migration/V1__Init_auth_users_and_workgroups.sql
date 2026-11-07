-- UC01..UC04: Bảng users, workgroups, user_workgroups
CREATE TABLE IF NOT EXISTS users (id BIGSERIAL PRIMARY KEY, email VARCHAR(254) UNIQUE NOT NULL, full_name VARCHAR(100) NOT NULL, password_hash VARCHAR(255) NOT NULL, site_role VARCHAR(32) NOT NULL, status VARCHAR(32) NOT NULL, failed_attempts INT DEFAULT 0, locked_until TIMESTAMP, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
