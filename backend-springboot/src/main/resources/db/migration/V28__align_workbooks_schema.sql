-- =====================================================================
-- V28: Align workbooks.name VARCHAR(255) with WorkbookEntity & UC08 spec
-- =====================================================================

ALTER TABLE workbooks
    ALTER COLUMN name TYPE VARCHAR(255);
