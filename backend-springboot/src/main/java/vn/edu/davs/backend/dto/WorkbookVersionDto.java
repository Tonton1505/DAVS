package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * API representation of a single workbook version entry (UC10).
 */
public record WorkbookVersionDto(
        UUID id,
        UUID workbookId,
        int versionNumber,
        String fileName,
        long sizeBytes,
        String note,
        UUID createdBy,
        String createdByName,
        Instant createdAt,
        boolean current,
        boolean purged
) {
}
