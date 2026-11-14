-- ============================================================================
-- V3__audit_logs_immutable.sql
-- Enforces append-only immutability on audit_logs (T-007)
-- ============================================================================

REVOKE UPDATE, DELETE ON audit_logs FROM PUBLIC;

CREATE OR REPLACE FUNCTION prevent_audit_logs_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  RAISE EXCEPTION 'audit_logs is append-only: % is forbidden', TG_OP
    USING ERRCODE = '42501';
END;
$$;

CREATE TRIGGER trg_audit_logs_immutable
  BEFORE UPDATE OR DELETE ON audit_logs
  FOR EACH ROW
  EXECUTE FUNCTION prevent_audit_logs_mutation();

CREATE INDEX ix_audit_logs_actor_email
  ON audit_logs (lower(actor_email), created_at DESC)
  WHERE actor_email IS NOT NULL;
