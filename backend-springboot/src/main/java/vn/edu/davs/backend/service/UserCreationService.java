package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.CreateUsersRequest;
import vn.edu.davs.backend.dto.CreateUsersResponse;
import vn.edu.davs.backend.dto.ImportJobResponse;
import vn.edu.davs.backend.dto.ImportJobStatusResponse;
import vn.edu.davs.backend.dto.LicensePoolDto;
import vn.edu.davs.backend.enu.AuthMethod;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.enu.SiteRole;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Core UC02 business logic for creating users individually/in batch and queueing CSV imports.
 */
public interface UserCreationService {

    int MAX_EMAIL_LENGTH = 254;
    Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");


    public static final String IMPORT_USERS_JOB_TYPE = "IMPORT_USERS";


    @Transactional
    CreateUsersResponse createUsers(CreateUsersRequest request, DavsUser actor);


    @Transactional
    ImportJobResponse enqueueCsvImport(
            MultipartFile file,
            AuthMethod authMethod,
            SiteRole defaultRole,
            DavsUser actor
    );


    @Transactional(readOnly = true)
    Optional<ImportJobStatusResponse> getImportStatus(UUID jobId);


    @Transactional(readOnly = true)
    Optional<String> getImportErrorReportCsv(UUID jobId);


    @Transactional(readOnly = true)
    List<LicensePoolDto> listLicensePools();


    ProvisionResult provisionSingleUser(
            String email,
            String fullName,
            AuthMethod authMethod,
            SiteRole role,
            UUID actorId
    );


    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        String trimmed = email.trim();
        return !trimmed.isEmpty()
                && trimmed.length() <= MAX_EMAIL_LENGTH
                && EMAIL_PATTERN.matcher(trimmed).matches();
    }


    public static List<String> extractDataLines(String csvContent) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String line;
            boolean firstNonEmpty = true;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.replace("\uFEFF", "").trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (firstNonEmpty) {
                    firstNonEmpty = false;
                    if (trimmed.toLowerCase().startsWith("email")) {
                        continue;
                    }
                }
                lines.add(trimmed);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed reading CSV content", ex);
        }
        return lines;
    }


    public record ProvisionResult(UUID userId, String status) {
    }
}
