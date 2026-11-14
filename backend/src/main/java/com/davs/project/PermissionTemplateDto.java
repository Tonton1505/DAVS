package com.davs.project;

import com.davs.permission.Capability;
import java.util.List;

/**
 * Preset permission template returned by {@code GET /api/v1/permission-templates}.
 */
public record PermissionTemplateDto(
        String id,
        String name,
        String description,
        List<Capability> capabilities
) {
}
