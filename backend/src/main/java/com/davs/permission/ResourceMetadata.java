package com.davs.permission;

import java.util.UUID;

/**
 * Ownership, parent hierarchy, and project permission mode resolved for a {@link ResourceRef}.
 *
 * @param resource       target resource reference
 * @param ownerId        user ID of the resource owner (may be {@code null})
 * @param parent         parent resource (e.g. parent project for a workbook, or parent workbook for a dashboard)
 * @param permissionMode permission mode (primarily for {@link ResourceType#PROJECT}; defaults to {@link PermissionMode#INHERIT})
 */
public record ResourceMetadata(
        ResourceRef resource,
        UUID ownerId,
        ResourceRef parent,
        PermissionMode permissionMode
) {
    public ResourceMetadata {
        if (permissionMode == null) {
            permissionMode = PermissionMode.INHERIT;
        }
    }

    public static ResourceMetadata of(
            ResourceRef resource,
            UUID ownerId,
            ResourceRef parent,
            PermissionMode permissionMode
    ) {
        return new ResourceMetadata(resource, ownerId, parent, permissionMode);
    }

    public static ResourceMetadata project(UUID projectId, UUID ownerId, UUID parentProjectId, PermissionMode mode) {
        ResourceRef parent = parentProjectId != null ? ResourceRef.project(parentProjectId) : null;
        return new ResourceMetadata(ResourceRef.project(projectId), ownerId, parent, mode);
    }
}
