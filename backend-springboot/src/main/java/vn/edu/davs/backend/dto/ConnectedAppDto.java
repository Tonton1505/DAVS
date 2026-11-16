package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Representation of a Connected Application (UC26).
 */
public record ConnectedAppDto(
        UUID id,
        String name,
        String trustType,
        String status,
        String clientId,
        String clientSecret,
        List<String> allowedDomains,
        String appUrl,
        UUID projectId,
        String permissionScope,
        UUID runAsUserId,
        Instant secretRotatedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
