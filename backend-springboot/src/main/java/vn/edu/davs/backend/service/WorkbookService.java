package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.RestoreWorkbookVersionRequest;
import vn.edu.davs.backend.dto.WorkbookDto;
import vn.edu.davs.backend.dto.WorkbookVersionDto;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Core business logic for UC08 Workbook Upload and UC10 Workbook Version History.
 */
public interface WorkbookService {


    @Transactional
    WorkbookDto uploadWorkbook(
            UUID projectId,
            String name,
            String description,
             boolean overwrite,
            String note,
            MultipartFile file,
            DavsUser actor
    );


    @Transactional(readOnly = true)
    WorkbookDto getWorkbook(UUID workbookId, DavsUser actor);


    @Transactional(readOnly = true)
    List<WorkbookDto> listProjectWorkbooks(UUID projectId, DavsUser actor);


    @Transactional(readOnly = true)
    List<WorkbookVersionDto> listVersions(UUID workbookId, DavsUser actor);


    @Transactional
    WorkbookVersionDto restoreVersion(
            UUID workbookId,
            int versionNumber,
            RestoreWorkbookVersionRequest request,
            DavsUser actor
    );


    @Transactional
    DownloadedVersion downloadVersion(UUID workbookId, int versionNumber, DavsUser actor);


    @Transactional
    void deleteVersion(UUID workbookId, int versionNumber, DavsUser actor);


    public record DownloadedVersion(String fileName, byte[] content) {
    }
}
