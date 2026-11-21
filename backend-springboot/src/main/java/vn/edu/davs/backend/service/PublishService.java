package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.PublishWorkbookRequest;
import vn.edu.davs.backend.dto.PublishWorkbookResponse;
import vn.edu.davs.backend.model.DavsUser;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementing UC21: Publish Workbook (FR-SHR-01, BUS-SHR-01..04).
 */
public interface PublishService {


    /**
     * Publishes a workbook to Internal Server, Cloud, or Public destination.
     */
    @Transactional
    PublishWorkbookResponse publishWorkbook(
            UUID workbookId,
            PublishWorkbookRequest request,
            DavsUser actor
    );


    /**
     * Lists available destination targets and default project info for publishing.
     */
    Map<String, Object> getPublishTargets(UUID workbookId, DavsUser actor);
}
