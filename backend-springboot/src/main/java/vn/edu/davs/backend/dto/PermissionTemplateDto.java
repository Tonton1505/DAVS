package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.Capability;
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
