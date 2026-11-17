-- ============================================================================
-- V26__performance_indexes.sql
-- T-103: Composite indexes for high-frequency queries & admin pagination (<= 2s)
-- ============================================================================

CREATE INDEX IF NOT EXISTS ix_audit_logs_created_at_desc
    ON audit_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS ix_workbooks_project_updated
    ON workbooks (project_id, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_dashboards_workbook_updated
    ON dashboards (workbook_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS ix_users_role_status_active
    ON users (site_role, status)
    WHERE deleted_at IS NULL;
