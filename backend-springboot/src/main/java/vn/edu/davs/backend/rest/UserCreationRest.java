package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.CreateUsersRequest;
import vn.edu.davs.backend.dto.CreateUsersResponse;
import vn.edu.davs.backend.dto.ImportJobResponse;
import vn.edu.davs.backend.dto.ImportJobStatusResponse;
import vn.edu.davs.backend.dto.LicensePoolDto;
import vn.edu.davs.backend.enu.AuthMethod;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.enu.SiteRole;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin-only REST endpoints for UC02 User Provisioning &amp; CSV Bulk Import ({@code /api/v1/users}).
 */
public interface UserCreationRest {


    @GetMapping("/licenses")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<List<LicensePoolDto>> listLicenses(@AuthenticationPrincipal DavsUser actor);


    @PostMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<CreateUsersResponse> createUsers(
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody CreateUsersRequest request
    );


    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<ImportJobResponse> importUsers(
            @AuthenticationPrincipal DavsUser actor,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "authMethod", defaultValue = "LOCAL") AuthMethod authMethod,
            @RequestParam(value = "defaultRole", defaultValue = "VIEWER") SiteRole defaultRole
    );


    @GetMapping("/import/{jobId}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<ImportJobStatusResponse> getImportStatus(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID jobId
    );


    @GetMapping("/import/{jobId}/errors")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<byte[]> downloadImportErrors(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable UUID jobId
    );


    @GetMapping("/import-template")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<byte[]> downloadTemplate(@AuthenticationPrincipal DavsUser actor);
}
