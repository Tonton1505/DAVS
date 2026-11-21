package vn.edu.davs.backend.hibernateDao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.davs.backend.dao.ResourcePermissionDao;
import vn.edu.davs.backend.dto.PermissionRuleDto;
import vn.edu.davs.backend.dto.PermissionSubjectDto;
import vn.edu.davs.backend.dto.UpdateResourcePermissionsRequest;
import vn.edu.davs.backend.entity.PermissionRuleEntity;
import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.Effect;
import vn.edu.davs.backend.enu.ResourceType;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.enu.SubjectType;
import vn.edu.davs.backend.util.HibernateSessionUtil;

/**
 * Hibernate Session implementation of {@link ResourcePermissionDao} for UC07.
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class HibernateResourcePermissionDao implements ResourcePermissionDao {

    @PersistenceContext
    private EntityManager entityManager;

    private Session session() {
        return HibernateSessionUtil.currentSession(entityManager);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionRuleDto> findRulesByResource(ResourceType type, UUID resourceId) {
        List<Object[]> rows = session().createNativeQuery(
                """
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
                WHERE r.resource_type = :resType AND r.resource_id = :resId
                ORDER BY r.subject_type ASC, lower(COALESCE(u.full_name, u.email, g.name, '')), r.capability ASC
                """,
                Object[].class
        )
                .setParameter("resType", type.name())
                .setParameter("resId", resourceId)
                .getResultList();

        List<PermissionRuleDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new PermissionRuleDto(
                    HibernateSessionUtil.toUuid(row[0]),
                    SubjectType.valueOf(String.valueOf(row[1])),
                    HibernateSessionUtil.toUuid(row[2]),
                    row[5] != null ? String.valueOf(row[5]) : null,
                    row[6] != null ? String.valueOf(row[6]) : null,
                    Capability.valueOf(String.valueOf(row[3])),
                    Effect.valueOf(String.valueOf(row[4]))
            ));
        }
        return result;
    }

    @Override
    public void replaceRules(
            ResourceType type,
            UUID resourceId,
            List<UpdateResourcePermissionsRequest.RuleInput> rules,
            UUID actorId
    ) {
        Session s = session();
        s.createMutationQuery(
                "DELETE FROM PermissionRuleEntity r WHERE r.resourceType = :resType AND r.resourceId = :resId"
        )
                .setParameter("resType", type.name())
                .setParameter("resId", resourceId)
                .executeUpdate();
        s.flush();

        if (rules == null || rules.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (var rule : rules) {
            PermissionRuleEntity entity = new PermissionRuleEntity();
            entity.setSubjectType(rule.subjectType().name());
            entity.setSubjectId(rule.subjectId());
            entity.setResourceType(type.name());
            entity.setResourceId(resourceId);
            entity.setCapability(rule.capability().name());
            entity.setEffect(rule.effect().name());
            entity.setCreatedAt(now);
            entity.setCreatedBy(actorId);
            s.persist(entity);
        }
        s.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSubjectRow> findActiveUser(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        List<Object[]> rows = session().createNativeQuery(
                """
                SELECT id, email, COALESCE(NULLIF(full_name, ''), email) AS display_name, site_role
                FROM users
                WHERE id = :userId AND deleted_at IS NULL AND status <> 'DISABLED'
                """,
                Object[].class
        )
                .setParameter("userId", userId)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        return Optional.of(new UserSubjectRow(
                HibernateSessionUtil.toUuid(row[0]),
                String.valueOf(row[1]),
                String.valueOf(row[2]),
                SiteRole.valueOf(String.valueOf(row[3]))
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveGroup(UUID groupId) {
        if (groupId == null) {
            return false;
        }
        Number count = session().createNativeQuery(
                "SELECT COUNT(*) FROM groups WHERE id = :groupId",
                Number.class
        )
                .setParameter("groupId", groupId)
                .getSingleResult();
        return count != null && count.longValue() > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionSubjectDto> listActiveSubjects() {
        Session s = session();
        List<Object[]> userRows = s.createNativeQuery(
                """
                SELECT id, COALESCE(NULLIF(full_name, ''), email) AS name, site_role AS detail
                FROM users
                WHERE deleted_at IS NULL AND status <> 'DISABLED'
                ORDER BY lower(COALESCE(NULLIF(full_name, ''), email)) ASC
                """,
                Object[].class
        ).getResultList();

        List<PermissionSubjectDto> result = new ArrayList<>();
        for (Object[] row : userRows) {
            result.add(new PermissionSubjectDto(
                    SubjectType.USER,
                    HibernateSessionUtil.toUuid(row[0]),
                    String.valueOf(row[1]),
                    String.valueOf(row[2])
            ));
        }

        List<Object[]> groupRows = s.createNativeQuery(
                """
                SELECT id, name
                FROM groups
                ORDER BY lower(name) ASC
                """,
                Object[].class
        ).getResultList();
        for (Object[] row : groupRows) {
            result.add(new PermissionSubjectDto(
                    SubjectType.GROUP,
                    HibernateSessionUtil.toUuid(row[0]),
                    String.valueOf(row[1]),
                    "GROUP"
            ));
        }
        return result;
    }
}
