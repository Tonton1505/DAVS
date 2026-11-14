package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.enu.PermissionMode;
import vn.edu.davs.backend.model.ResourceMetadata;
import vn.edu.davs.backend.model.ResourceRef;
import vn.edu.davs.backend.enu.ResourceType;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Resolves {@link ResourceMetadata} for {@link ResourceType#PROJECT} from {@code projects}.
 */
@Component
@RequiredArgsConstructor
public class ProjectResourceMetadataResolver implements ResourceMetadataResolver {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public ResourceType supports() {
        return ResourceType.PROJECT;
    }

    @Override
    public Optional<ResourceMetadata> resolve(UUID resourceId) {
        if (resourceId == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, owner_id, parent_id, permission_mode
                FROM projects
                WHERE id = ? AND deleted_at IS NULL
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UUID id = rs.getObject("id", UUID.class);
            UUID ownerId = rs.getObject("owner_id", UUID.class);
            UUID parentId = rs.getObject("parent_id", UUID.class);
            String rawMode = rs.getString("permission_mode");
            PermissionMode mode = mapMode(rawMode);
            ResourceRef parentRef = parentId != null ? new ResourceRef(ResourceType.PROJECT, parentId) : null;
            return new ResourceMetadata(
                    new ResourceRef(ResourceType.PROJECT, id),
                    ownerId,
                    parentRef,
                    mode
            );
        }, resourceId).stream().findFirst();
    }

    public static PermissionMode mapMode(String rawMode) {
        if (rawMode == null || "CUSTOMIZABLE".equalsIgnoreCase(rawMode)) {
            return PermissionMode.CUSTOM;
        }
        return PermissionMode.valueOf(rawMode.toUpperCase());
    }
}
