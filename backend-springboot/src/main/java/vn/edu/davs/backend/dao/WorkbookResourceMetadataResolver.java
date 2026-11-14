package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.enu.PermissionMode;
import vn.edu.davs.backend.enu.ResourceType;
import vn.edu.davs.backend.model.ResourceMetadata;
import vn.edu.davs.backend.model.ResourceRef;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Resolves {@link ResourceMetadata} for {@link ResourceType#WORKBOOK} from {@code workbooks} (BUS-PRJ-12).
 */
@Component
@RequiredArgsConstructor
public class WorkbookResourceMetadataResolver implements ResourceMetadataResolver {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public ResourceType supports() {
        return ResourceType.WORKBOOK;
    }

    @Override
    public Optional<ResourceMetadata> resolve(UUID resourceId) {
        if (resourceId == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, owner_id, project_id
                FROM workbooks
                WHERE id = ? AND deleted_at IS NULL
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UUID id = rs.getObject("id", UUID.class);
            UUID ownerId = rs.getObject("owner_id", UUID.class);
            UUID projectId = rs.getObject("project_id", UUID.class);
            ResourceRef parentRef = projectId != null ? new ResourceRef(ResourceType.PROJECT, projectId) : null;
            return new ResourceMetadata(
                    new ResourceRef(ResourceType.WORKBOOK, id),
                    ownerId,
                    parentRef,
                    PermissionMode.INHERIT
            );
        }, resourceId).stream().findFirst();
    }
}
