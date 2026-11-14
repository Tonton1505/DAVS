package com.davs.permission;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed reference to a protected DAVS resource.
 */
public record ResourceRef(ResourceType type, UUID id) {

    public ResourceRef {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(id, "id must not be null");
    }

    public static ResourceRef project(UUID id) {
        return new ResourceRef(ResourceType.PROJECT, id);
    }

    public static ResourceRef workbook(UUID id) {
        return new ResourceRef(ResourceType.WORKBOOK, id);
    }

    public static ResourceRef datasource(UUID id) {
        return new ResourceRef(ResourceType.DATASOURCE, id);
    }

    public static ResourceRef dashboard(UUID id) {
        return new ResourceRef(ResourceType.DASHBOARD, id);
    }
}
