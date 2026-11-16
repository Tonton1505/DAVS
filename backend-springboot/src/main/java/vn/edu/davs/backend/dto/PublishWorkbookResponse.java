package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response payload for UC21: Publishing a workbook.
 */
public record PublishWorkbookResponse(
        UUID id,
        UUID workbookId,
        UUID projectId,
        String destination,
        String publishedName,
        String url,
        int version,
        List<String> warnings,
        Instant publishedAt
) {
}
