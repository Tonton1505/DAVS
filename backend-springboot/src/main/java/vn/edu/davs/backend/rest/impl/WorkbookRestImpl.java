package vn.edu.davs.backend.rest.impl;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.davs.backend.rest.WorkbookRest;

@RequiredArgsConstructor
@RestController
public class WorkbookRestImpl implements WorkbookRest {


    private final WorkbookService workbookService;

    @Override
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

    @Override
    public ResponseEntity<List<WorkbookDto>> listProjectWorkbooks(
            @PathVariable("id") UUID projectId,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(workbookService.listProjectWorkbooks(projectId, actor));
    }

    @Override
    public ResponseEntity<WorkbookDto> getWorkbook(
            @PathVariable("id") UUID workbookId,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(workbookService.getWorkbook(workbookId, actor));
    }

    @Override
    public ResponseEntity<List<WorkbookVersionDto>> listVersions(
            @PathVariable("id") UUID workbookId,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(workbookService.listVersions(workbookId, actor));
    }

    @Override
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

    @Override
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

    @Override
    public ResponseEntity<Void> deleteVersion(
            @PathVariable("id") UUID workbookId,
            @PathVariable("v") int versionNumber,
            @AuthenticationPrincipal DavsUser actor
    ) {
        workbookService.deleteVersion(workbookId, versionNumber, actor);
        return ResponseEntity.noContent().build();
    }
}
