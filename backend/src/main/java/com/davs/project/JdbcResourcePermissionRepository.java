package com.davs.project;

import com.davs.common.security.SiteRole;
import com.davs.permission.Capability;
import com.davs.permission.Effect;
import com.davs.permission.ResourceType;
import com.davs.permission.SubjectType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC repository for UC07 permission rule management and active subject lookups.
 */
@Repository
@RequiredArgsConstructor
public class JdbcResourcePermissionRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<PermissionRuleDto> findRulesByResource(ResourceType type, UUID resourceId) {
        String sql = """
                SELECT r.id, r.subject_type, r.subject_id, r.capability, r.effect,
                       COALESCE(NULLIF(u.full_name, ''), u.email, g.name, r.subject_id::text) AS subject_name,
                       CASE
                         WHEN r.subject_type = 'USER' THEN u.site_role
                         ELSE 'GROUP'
                       END AS subject_detail
                FROM permission_rules r
                LEFT JOIN users u
                  ON r.subject_type = 'USER' AND u.id = r.subject_id AND u.deleted_at IS NULL
                LEFT JOIN groups g
                  ON r.subject_type = 'GROUP' AND g.id = r.subject_id
                WHERE r.resource_type = ? AND r.resource_id = ?
                ORDER BY r.subject_type ASC, lower(COALESCE(u.full_name, u.email, g.name, '')), r.capability ASC
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new PermissionRuleDto(
                        rs.getObject("id", UUID.class),
                        SubjectType.valueOf(rs.getString("subject_type")),
                        rs.getObject("subject_id", UUID.class),
                        rs.getString("subject_name"),
                        rs.getString("subject_detail"),
                        Capability.valueOf(rs.getString("capability")),
                        Effect.valueOf(rs.getString("effect"))
                ),
                type.name(),
                resourceId
        );
    }

    public void replaceRules(
            ResourceType type,
            UUID resourceId,
            List<UpdateResourcePermissionsRequest.RuleInput> rules,
            UUID actorId
    ) {
        jdbcTemplate.update(
                "DELETE FROM permission_rules WHERE resource_type = ? AND resource_id = ?",
                type.name(),
                resourceId
        );
        if (rules == null || rules.isEmpty()) {
            return;
        }
        String insertSql = """
                INSERT INTO permission_rules (
                    subject_type, subject_id, resource_type, resource_id, capability, effect, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        List<Object[]> batchArgs = new ArrayList<>(rules.size());
        for (var rule : rules) {
            batchArgs.add(new Object[]{
                    rule.subjectType().name(),
                    rule.subjectId(),
                    type.name(),
                    resourceId,
                    rule.capability().name(),
                    rule.effect().name(),
                    actorId
            });
        }
        jdbcTemplate.batchUpdate(insertSql, batchArgs);
    }

    public Optional<UserSubjectRow> findActiveUser(UUID userId) {
        String sql = """
                SELECT id, email, COALESCE(NULLIF(full_name, ''), email) AS display_name, site_role
                FROM users
                WHERE id = ? AND deleted_at IS NULL AND status <> 'DISABLED'
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new UserSubjectRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        SiteRole.valueOf(rs.getString("site_role"))
                ),
                userId
        ).stream().findFirst();
    }

    public boolean existsActiveGroup(UUID groupId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM groups WHERE id = ?",
                Integer.class,
                groupId
        );
        return count != null && count > 0;
    }

    public List<PermissionSubjectDto> listActiveSubjects() {
        String usersSql = """
                SELECT id, COALESCE(NULLIF(full_name, ''), email) AS name, site_role AS detail
                FROM users
                WHERE deleted_at IS NULL AND status <> 'DISABLED'
                ORDER BY lower(COALESCE(NULLIF(full_name, ''), email)) ASC
                """;
        List<PermissionSubjectDto> result = new ArrayList<>();
        result.addAll(jdbcTemplate.query(
                usersSql,
                (rs, rowNum) -> new PermissionSubjectDto(
                        SubjectType.USER,
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("detail")
                )
        ));

        String groupsSql = """
                SELECT id, name
                FROM groups
                ORDER BY lower(name) ASC
                """;
        result.addAll(jdbcTemplate.query(
                groupsSql,
                (rs, rowNum) -> new PermissionSubjectDto(
                        SubjectType.GROUP,
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        "GROUP"
                )
        ));
        return result;
    }

    /**
     * Projection of an active user row for permission validation and effective-permission lookup.
     */
    public record UserSubjectRow(UUID id, String email, String displayName, SiteRole siteRole) {
    }
}
