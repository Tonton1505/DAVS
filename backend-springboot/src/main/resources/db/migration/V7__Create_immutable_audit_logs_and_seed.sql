-- NFR-SEC-05: Bảng nhật ký kiểm toán bất biến (audit_logs)
CREATE TABLE IF NOT EXISTS audit_logs (id BIGSERIAL PRIMARY KEY, actor_email VARCHAR(254) NOT NULL, action VARCHAR(64) NOT NULL, details TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
