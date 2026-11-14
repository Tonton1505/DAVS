package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * API representation of a DAVS workbook (UC08 &amp; UC10).
 */
public record WorkbookDto(
        UUID id,
        UUID projectId,
        String projectName,
        String name,
        String description,
        int currentVersion,
        UUID ownerId,
        String ownerName,
        String importStatus,
        boolean needsReconnect,
        long sizeBytes,
        String fileName,
        List<LabelDto> labels,
        Instant createdAt,
        Instant updatedAt
) {
}
