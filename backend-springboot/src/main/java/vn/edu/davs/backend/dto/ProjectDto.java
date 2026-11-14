package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * API representation of a DAVS project folder.
 */
public record ProjectDto(
        UUID id,
        String name,
        String description,
        UUID parentId,
        UUID ownerId,
        String permissionMode,
        boolean isDefault,
        int depth,
        Instant createdAt,
        Instant updatedAt
) {
}
