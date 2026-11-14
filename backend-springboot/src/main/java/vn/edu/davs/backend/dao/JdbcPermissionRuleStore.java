package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.Effect;
import vn.edu.davs.backend.enu.ResourceType;
import vn.edu.davs.backend.enu.SubjectType;
import vn.edu.davs.backend.model.PermissionRule;
import vn.edu.davs.backend.model.ResourceRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC-backed {@link PermissionRuleStore} reading from {@code permission_rules}.
 */
@Repository
@RequiredArgsConstructor
public class JdbcPermissionRuleStore implements PermissionRuleStore {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<PermissionRule> findMatchingRules(ResourceRef resource, UUID userId, Set<UUID> groupIds) {
        if (resource == null || userId == null) {
            return List.of();
        }
        Set<UUID> groups = groupIds == null ? Set.of() : groupIds;

        StringBuilder sql = new StringBuilder(
                "SELECT id, subject_type, subject_id, resource_type, resource_id, capability, effect "
                        + "FROM permission_rules "
                        + "WHERE resource_type = ? AND resource_id = ? "
                        + "AND ((subject_type = 'USER' AND subject_id = ?)"
        );

        List<Object> params = new ArrayList<>();
        params.add(resource.type().name());
        params.add(resource.id());
        params.add(userId);

        if (!groups.isEmpty()) {
            String placeholders = String.join(",", Collections.nCopies(groups.size(), "?"));
            sql.append(" OR (subject_type = 'GROUP' AND subject_id IN (").append(placeholders).append("))");
            params.addAll(groups);
        }
        sql.append(")");

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new PermissionRule(
                        rs.getObject("id", UUID.class),
                        SubjectType.valueOf(rs.getString("subject_type")),
                        rs.getObject("subject_id", UUID.class),
                        ResourceType.valueOf(rs.getString("resource_type")),
                        rs.getObject("resource_id", UUID.class),
                        Capability.valueOf(rs.getString("capability")),
                        Effect.valueOf(rs.getString("effect"))
                ),
                params.toArray()
        );
    }
}
