package com.davs.user;

import com.davs.common.security.SiteRole;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC persistence for UC02 user creation, license reservation, invitations, and CSV import jobs.
 */
@Repository
@RequiredArgsConstructor
public class JdbcUserCreationRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean existsActiveEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE lower(email) = lower(?) AND deleted_at IS NULL",
                Integer.class,
                email
        );
        return count != null && count > 0;
    }

    public List<LicensePoolDto> listLicensePools() {
        String sql = """
                SELECT role, total, used
                FROM license_pools
                ORDER BY CASE role
                    WHEN 'SITE_ADMIN' THEN 1
                    WHEN 'CREATOR' THEN 2
                    WHEN 'EXPLORER' THEN 3
                    ELSE 4
                END
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            int total = rs.getInt("total");
            int used = rs.getInt("used");
            return new LicensePoolDto(
                    SiteRole.valueOf(rs.getString("role")),
                    total,
                    used,
                    Math.max(0, total - used)
            );
        });
    }

    public boolean reserveLicenses(SiteRole role, int count) {
        if (count <= 0) {
            return true;
        }
        int updated = jdbcTemplate.update(
                """
                UPDATE license_pools
                SET used = used + ?, updated_at = now()
                WHERE role = ? AND used + ? <= total
                """,
                count,
                role.name(),
                count
        );
        return updated == 1;
    }

    public UUID insertInvitedUser(
            String email,
            String passwordHash,
            String fullName,
            AuthMethod authMethod,
            SiteRole siteRole,
            UUID createdBy
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO users (
                    email, password_hash, full_name, auth_method, site_role,
                    status, must_change_password, created_by
                )
                VALUES (?, ?, ?, ?, ?, 'PENDING', TRUE, ?)
                RETURNING id
                """,
                UUID.class,
                email,
                passwordHash,
                fullName,
                authMethod.name(),
                siteRole.name(),
                createdBy
        );
    }

    public void updateUserStatus(UUID userId, String status) {
        jdbcTemplate.update(
                "UPDATE users SET status = ?, updated_at = now() WHERE id = ?",
                status,
                userId
        );
    }

    public void insertInvitation(UUID userId, String tokenHash, Instant expiresAt, UUID createdBy) {
        jdbcTemplate.update(
                """
                INSERT INTO invitations (user_id, token_hash, expires_at, created_by)
                VALUES (?, ?, ?, ?)
                """,
                userId,
                tokenHash,
                Timestamp.from(expiresAt),
                createdBy
        );
    }

    public void createImportJobRecord(UUID jobId, AuthMethod authMethod, int totalRows, UUID createdBy) {
        jdbcTemplate.update(
                """
                INSERT INTO user_import_jobs (job_id, auth_method, status, total_rows, created_by)
                VALUES (?, ?, 'PENDING', ?, ?)
                """,
                jobId,
                authMethod.name(),
                totalRows,
                createdBy
        );
    }

    public void markImportJobRunning(UUID jobId) {
        jdbcTemplate.update(
                "UPDATE user_import_jobs SET status = 'RUNNING', updated_at = now() WHERE job_id = ?",
                jobId
        );
    }

    public void completeImportJob(
            UUID jobId,
            String status,
            int createdCount,
            int skippedCount,
            int errorCount,
            String errorCsv
    ) {
        jdbcTemplate.update(
                """
                UPDATE user_import_jobs
                SET status = ?,
                    created_count = ?,
                    skipped_count = ?,
                    error_count = ?,
                    error_csv = ?,
                    updated_at = now()
                WHERE job_id = ?
                """,
                status,
                createdCount,
                skippedCount,
                errorCount,
                errorCsv,
                jobId
        );
    }

    public Optional<ImportJobStatusResponse> findImportJobStatus(UUID jobId) {
        String sql = """
                SELECT job_id, status, total_rows, created_count, skipped_count, error_count,
                       (error_csv IS NOT NULL AND length(error_csv) > 0) AS has_error_report
                FROM user_import_jobs
                WHERE job_id = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ImportJobStatusResponse(
                rs.getObject("job_id", UUID.class),
                rs.getString("status"),
                rs.getInt("total_rows"),
                rs.getInt("created_count"),
                rs.getInt("skipped_count"),
                rs.getInt("error_count"),
                rs.getBoolean("has_error_report")
        ), jobId).stream().findFirst();
    }

    public Optional<String> findImportErrorCsv(UUID jobId) {
        List<String> results = jdbcTemplate.query(
                "SELECT error_csv FROM user_import_jobs WHERE job_id = ?",
                (rs, rowNum) -> rs.getString("error_csv"),
                jobId
        );
        if (results.isEmpty() || results.get(0) == null || results.get(0).isBlank()) {
            return Optional.empty();
        }
        return Optional.of(results.get(0));
    }
}
