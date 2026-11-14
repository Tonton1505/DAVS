-- ============================================================================
-- V4__seed_system_settings.sql
-- Default configurable system limits for DAVS (T-008, context.md section 8)
-- ============================================================================

INSERT INTO system_settings (setting_key, setting_value, value_type, description)
VALUES
  ('session.idleMinutes', '30', 'INT', 'Idle session timeout in minutes before re-authentication is required'),
  ('login.maxFailures', '5', 'INT', 'Maximum consecutive failed login attempts before temporary lockout'),
  ('login.lockMinutes', '15', 'INT', 'Account lockout duration in minutes after exceeding login.maxFailures'),
  ('upload.maxBytes', '1073741824', 'LONG', 'Maximum upload file size in bytes (1 GB)'),
  ('workbook.maxVersions', '25', 'INT', 'Maximum retained versions per workbook'),
  ('export.maxRows', '500000', 'INT', 'Maximum rows allowed in a single export'),
  ('export.maxBytes', '104857600', 'LONG', 'Maximum export file size in bytes (100 MB)'),
  ('chart.maxMarks', '100000', 'INT', 'Maximum marks rendered in a single chart'),
  ('trash.retentionDays', '30', 'INT', 'Number of days soft-deleted items remain in trash before purge'),
  ('audit.retentionMonths', '12', 'INT', 'Minimum retention period in months for audit_logs'),
  ('import.maxUsersPerFile', '1000', 'INT', 'Maximum user rows allowed per bulk CSV/Excel import file'),
  ('refresh.maxConcurrent', '4', 'INT', 'Maximum concurrent extract refresh jobs'),
  ('refresh.pauseAfterFailures', '5', 'INT', 'Consecutive refresh failures before auto-pausing schedule'),
  ('comment.editWindowHours', '24', 'INT', 'Hours after posting during which a comment can be edited'),
  ('share.maxRecipients', '100', 'INT', 'Maximum recipients per share action'),
  ('project.maxDepth', '5', 'INT', 'Maximum nesting depth of project folders'),
  ('publish.publicEnabled', 'false', 'BOOLEAN', 'Whether public/anonymous publishing is enabled')
ON CONFLICT (setting_key) DO NOTHING;
