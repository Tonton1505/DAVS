package vn.edu.davs.backend.dto;

import java.util.UUID;

/**
 * Directory item for recipient lookup in sharing dialog (URD-15).
 */
public record DirectoryItemDto(
        UUID id,
        String type,
        String name,
        String email,
        String avatar,
        boolean isExternal
) {
}
