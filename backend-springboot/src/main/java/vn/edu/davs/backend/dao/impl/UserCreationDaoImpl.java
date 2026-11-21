package vn.edu.davs.backend.dao.impl;

import vn.edu.davs.backend.dto.ImportJobStatusResponse;
import vn.edu.davs.backend.dto.LicensePoolDto;
import vn.edu.davs.backend.enu.AuthMethod;
import vn.edu.davs.backend.enu.SiteRole;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.edu.davs.backend.dao.IUserCreationDao;

@RequiredArgsConstructor
@Repository
public class UserCreationDaoImpl implements IUserCreationDao {


    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean existsActiveEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE lower(email) = lower(?) AND deleted_at IS NULL",
                Integer.class,
                email
        );
        return count != null && count > 0;
    }

    @Override
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

    @Override
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

    @Override
    public UUID insertInvitedUser(
            String email,
            String passwordHash,
            String fullName,
            AuthMethod authMethod,
            SiteRole siteRole,
            UUID createdBy
    ) {
        UUID userId = jdbcTemplate.queryForObject(
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
        if (userId != null) {
            jdbcTemplate.update(
                    """
                    INSERT INTO group_members (group_id, user_id)
                    SELECT id, ? FROM groups WHERE lower(name) = lower('All Users')
                    ON CONFLICT (group_id, user_id) DO NOTHING
                    """,
                    userId
            );
        }
        return userId;
    }

    @Override
    public void updateUserStatus(UUID userId, String status) {
        jdbcTemplate.update(
                "UPDATE users SET status = ?, updated_at = now() WHERE id = ?",
                status,
                userId
        );
    }

    @Override
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

    @Override
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

    @Override
    public void markImportJobRunning(UUID jobId) {
        jdbcTemplate.update(
                "UPDATE user_import_jobs SET status = 'RUNNING', updated_at = now() WHERE job_id = ?",
                jobId
        );
    }

    @Override
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

    @Override
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

    @Override
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
