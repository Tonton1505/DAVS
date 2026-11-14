package com.davs.permission;

import java.util.Optional;
import java.util.UUID;

/**
 * SPI implemented by domain modules (project, workbook, datasource, viz) to supply
 * ownership, parent hierarchy, and {@link PermissionMode} for their resources.
 */
public interface ResourceMetadataResolver {

    /**
     * Returns the {@link ResourceType} handled by this resolver.
     */
    ResourceType supports();

    /**
     * Resolves metadata for the given resource ID.
     */
    Optional<ResourceMetadata> resolve(UUID resourceId);
}
