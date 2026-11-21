package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.RestoreWorkbookVersionRequest;
import vn.edu.davs.backend.dto.WorkbookDto;
import vn.edu.davs.backend.dto.WorkbookVersionDto;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.UUID;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * REST endpoints for UC08 Workbook Upload and UC10 Workbook Version History.
 */
@RequestMapping("/api/v1")
public interface WorkbookRest {


    @PostMapping(
            value = "/projects/{id}/workbooks/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    ResponseEntity<WorkbookDto> uploadWorkbook(
            @PathVariable("id") UUID projectId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "overwrite", required = false, defaultValue = "false") boolean overwrite,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal DavsUser actor
    );


    @GetMapping("/projects/{id}/workbooks")
    ResponseEntity<List<WorkbookDto>> listProjectWorkbooks(
            @PathVariable("id") UUID projectId,
            @AuthenticationPrincipal DavsUser actor
    );


    @GetMapping("/workbooks/{id}")
    ResponseEntity<WorkbookDto> getWorkbook(
            @PathVariable("id") UUID workbookId,
            @AuthenticationPrincipal DavsUser actor
    );


    @GetMapping("/workbooks/{id}/versions")
    ResponseEntity<List<WorkbookVersionDto>> listVersions(
            @PathVariable("id") UUID workbookId,
            @AuthenticationPrincipal DavsUser actor
    );


    @PostMapping("/workbooks/{id}/versions/{v}/restore")
    ResponseEntity<WorkbookVersionDto> restoreVersion(
            @PathVariable("id") UUID workbookId,
            @PathVariable("v") int versionNumber,
            @RequestBody(required = false) RestoreWorkbookVersionRequest request,
            @AuthenticationPrincipal DavsUser actor
    );


    @GetMapping("/workbooks/{id}/versions/{v}/download")
    ResponseEntity<byte[]> downloadVersion(
            @PathVariable("id") UUID workbookId,
            @PathVariable("v") int versionNumber,
            @AuthenticationPrincipal DavsUser actor
    );


    @DeleteMapping("/workbooks/{id}/versions/{v}")
    ResponseEntity<Void> deleteVersion(
            @PathVariable("id") UUID workbookId,
            @PathVariable("v") int versionNumber,
            @AuthenticationPrincipal DavsUser actor
    );
}
