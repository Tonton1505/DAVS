package vn.edu.davs.backend.rest.impl;

import vn.edu.davs.backend.dto.CreateUsersRequest;
import vn.edu.davs.backend.dto.CreateUsersResponse;
import vn.edu.davs.backend.dto.ImportJobResponse;
import vn.edu.davs.backend.dto.ImportJobStatusResponse;
import vn.edu.davs.backend.dto.LicensePoolDto;
import vn.edu.davs.backend.enu.AuthMethod;
import vn.edu.davs.backend.service.UserCreationService;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.service.PermissionService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.davs.backend.rest.UserCreationRest;

@RequiredArgsConstructor
@RestController
public class UserCreationRestImpl implements UserCreationRest {


    private static final String TEMPLATE_CSV =
            "email,full_name,role\nan.nguyen@congty.vn,Nguyễn Văn An,EXPLORER\n";


    private final UserCreationService userCreationService;

    private final PermissionService permissionService;

    @Override
    public ResponseEntity<List<LicensePoolDto>> listLicenses(@AuthenticationPrincipal DavsUser actor) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(userCreationService.listLicensePools());
    }

    @Override
    public ResponseEntity<CreateUsersResponse> createUsers(
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody CreateUsersRequest request
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        CreateUsersResponse result = userCreationService.createUsers(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Override
    public ResponseEntity<ImportJobResponse> importUsers(
            @AuthenticationPrincipal DavsUser actor,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "authMethod", defaultValue = "LOCAL") AuthMethod authMethod,
            @RequestParam(value = "defaultRole", defaultValue = "VIEWER") SiteRole defaultRole
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        ImportJobResponse job = userCreationService.enqueueCsvImport(file, authMethod, defaultRole, actor);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    @Override
    public ResponseEntity<ImportJobStatusResponse> getImportStatus(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID jobId
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        ImportJobStatusResponse status = userCreationService.getImportStatus(jobId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_SYS_01, jobId.toString()));
        return ResponseEntity.ok(status);
    }

    @Override
    public ResponseEntity<byte[]> downloadImportErrors(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID jobId
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        String csv = userCreationService.getImportErrorReportCsv(jobId)
                .orElse("line,email,error_code,message\n");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"import-errors-" + jobId + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public ResponseEntity<byte[]> downloadTemplate(@AuthenticationPrincipal DavsUser actor) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users-import-template.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(TEMPLATE_CSV.getBytes(StandardCharsets.UTF_8));
    }
}
