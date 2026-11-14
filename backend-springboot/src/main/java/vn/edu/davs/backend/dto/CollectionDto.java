package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Personal collection representation for UC06 [6d].
 */
public record CollectionDto(
        UUID id,
        String name,
        String description,
        UUID ownerId,
        int itemCount,
        List<CollectionItemDto> items,
        Instant createdAt,
        Instant updatedAt
) {
    public record CollectionItemDto(
            UUID collectionId,
            String resourceType,
            UUID resourceId,
            String resourceName,
            Instant addedAt
    ) {
    }
}
