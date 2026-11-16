package vn.edu.davs.backend.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for UC21: Publishing a workbook.
 */
public record PublishWorkbookRequest(
        String destination,
        UUID projectId,
        String name,
        String description,
        List<String> visibleSheets,
        String dataSourceMode,
        boolean packagedTwbx,
        boolean hasEmbeddedCredentials,
        boolean overwrite
) {
}
