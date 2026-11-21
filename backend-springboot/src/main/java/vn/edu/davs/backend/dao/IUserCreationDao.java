package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.ImportJobStatusResponse;
import vn.edu.davs.backend.dto.LicensePoolDto;
import vn.edu.davs.backend.enu.AuthMethod;
import vn.edu.davs.backend.enu.SiteRole;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC persistence for UC02 user creation, license reservation, invitations, and CSV import jobs.
 */
public interface IUserCreationDao {


    boolean existsActiveEmail(String email);


    List<LicensePoolDto> listLicensePools();


    boolean reserveLicenses(SiteRole role, int count);


    UUID insertInvitedUser(
            String email,
            String passwordHash,
            String fullName,
            AuthMethod authMethod,
            SiteRole siteRole,
            UUID createdBy
    );


    void updateUserStatus(UUID userId, String status);


    void insertInvitation(UUID userId, String tokenHash, Instant expiresAt, UUID createdBy);


    void createImportJobRecord(UUID jobId, AuthMethod authMethod, int totalRows, UUID createdBy);


    void markImportJobRunning(UUID jobId);


    void completeImportJob(
            UUID jobId,
            String status,
            int createdCount,
            int skippedCount,
            int errorCount,
            String errorCsv
    );


    Optional<ImportJobStatusResponse> findImportJobStatus(UUID jobId);


    Optional<String> findImportErrorCsv(UUID jobId);
}
