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
 * Resolves {@link ResourceMetadata} for {@link ResourceType#DASHBOARD} from {@code dashboards}.
 */
@Component
@RequiredArgsConstructor
public class DashboardResourceMetadataResolver implements ResourceMetadataResolver {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public ResourceType supports() {
        return ResourceType.DASHBOARD;
    }

    @Override
    public Optional<ResourceMetadata> resolve(UUID resourceId) {
        if (resourceId == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT d.id, d.created_by, d.workbook_id, w.owner_id AS workbook_owner_id
                FROM dashboards d
                JOIN workbooks w ON w.id = d.workbook_id
                WHERE d.id = ? AND d.deleted_at IS NULL AND w.deleted_at IS NULL
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UUID id = rs.getObject("id", UUID.class);
            UUID createdBy = rs.getObject("created_by", UUID.class);
            UUID workbookOwnerId = rs.getObject("workbook_owner_id", UUID.class);
            UUID ownerId = createdBy != null ? createdBy : workbookOwnerId;
            UUID workbookId = rs.getObject("workbook_id", UUID.class);
            ResourceRef parentRef = workbookId != null ? new ResourceRef(ResourceType.WORKBOOK, workbookId) : null;
            return new ResourceMetadata(
                    new ResourceRef(ResourceType.DASHBOARD, id),
                    ownerId,
                    parentRef,
                    PermissionMode.INHERIT
            );
        }, resourceId).stream().findFirst();
    }
}
