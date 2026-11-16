package vn.edu.davs.backend.controller;

import vn.edu.davs.backend.dto.RestoreWorkbookVersionRequest;
import vn.edu.davs.backend.dto.WorkbookDto;
import vn.edu.davs.backend.dto.WorkbookVersionDto;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.WorkbookService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST endpoints for UC08 Workbook Upload and UC10 Workbook Version History.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorkbookController {

    private final WorkbookService workbookService;

    @PostMapping(
            value = "/projects/{id}/workbooks/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<WorkbookDto> uploadWorkbook(
            @PathVariable("id") UUID projectId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "overwrite", required = false, defaultValue = "false") boolean overwrite,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal DavsUser actor
    ) {
        WorkbookDto uploaded = workbookService.uploadWorkbook(
                projectId,
                name,
                description,
                overwrite,
                note,
                file,
                actor
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(uploaded);
    }

    @GetMapping("/projects/{id}/workbooks")
    public ResponseEntity<List<WorkbookDto>> listProjectWorkbooks(
            @PathVariable("id") UUID projectId,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(workbookService.listProjectWorkbooks(projectId, actor));
    }

    @GetMapping("/workbooks/{id}")
    public ResponseEntity<WorkbookDto> getWorkbook(
            @PathVariable("id") UUID workbookId,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(workbookService.getWorkbook(workbookId, actor));
    }

    @GetMapping("/workbooks/{id}/versions")
    public ResponseEntity<List<WorkbookVersionDto>> listVersions(
            @PathVariable("id") UUID workbookId,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(workbookService.listVersions(workbookId, actor));
    }

    @PostMapping("/workbooks/{id}/versions/{v}/restore")
    public ResponseEntity<WorkbookVersionDto> restoreVersion(
            @PathVariable("id") UUID workbookId,
            @PathVariable("v") int versionNumber,
            @RequestBody(required = false) RestoreWorkbookVersionRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        WorkbookVersionDto restored = workbookService.restoreVersion(
                workbookId,
                versionNumber,
                request,
                actor
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(restored);
    }

    @GetMapping("/workbooks/{id}/versions/{v}/download")
    public ResponseEntity<byte[]> downloadVersion(
            @PathVariable("id") UUID workbookId,
            @PathVariable("v") int versionNumber,
            @AuthenticationPrincipal DavsUser actor
    ) {
        WorkbookService.DownloadedVersion downloaded = workbookService.downloadVersion(
                workbookId,
                versionNumber,
                actor
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(downloaded.fileName()).build()
        );
        return new ResponseEntity<>(downloaded.content(), headers, HttpStatus.OK);
    }

    @DeleteMapping("/workbooks/{id}/versions/{v}")
    public ResponseEntity<Void> deleteVersion(
            @PathVariable("id") UUID workbookId,
            @PathVariable("v") int versionNumber,
            @AuthenticationPrincipal DavsUser actor
    ) {
        workbookService.deleteVersion(workbookId, versionNumber, actor);
        return ResponseEntity.noContent().build();
    }
}
