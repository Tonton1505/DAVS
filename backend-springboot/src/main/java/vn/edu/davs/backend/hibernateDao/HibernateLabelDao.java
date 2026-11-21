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
import vn.edu.davs.backend.dao.LabelDao;
import vn.edu.davs.backend.dto.LabelCategoryDto;
import vn.edu.davs.backend.dto.LabelDto;
import vn.edu.davs.backend.entity.LabelAssignmentEntity;
import vn.edu.davs.backend.entity.LabelCategoryEntity;
import vn.edu.davs.backend.entity.LabelEntity;
import vn.edu.davs.backend.util.HibernateSessionUtil;

/**
 * Hibernate Session implementation of {@link LabelDao} for UC09.
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class HibernateLabelDao implements LabelDao {

    @PersistenceContext
    private EntityManager entityManager;

    private Session session() {
        return HibernateSessionUtil.currentSession(entityManager);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelCategoryDto> listCategories() {
        return session()
                .createSelectionQuery(
                        "FROM LabelCategoryEntity c ORDER BY c.warning DESC, lower(c.name) ASC",
                        LabelCategoryEntity.class
                )
                .getResultList()
                .stream()
                .map(HibernateLabelDao::toCategoryDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LabelCategoryDto> findCategoryById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return session()
                .createSelectionQuery("FROM LabelCategoryEntity c WHERE c.id = :id", LabelCategoryEntity.class)
                .setParameter("id", id)
                .uniqueResultOptional()
                .map(HibernateLabelDao::toCategoryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsCategoryByName(String name) {
        Long count = session()
                .createSelectionQuery(
                        "SELECT COUNT(c) FROM LabelCategoryEntity c WHERE lower(c.name) = lower(:name)",
                        Long.class
                )
                .setParameter("name", name)
                .uniqueResult();
        return count != null && count > 0;
    }

    @Override
    public UUID insertCategory(String name, String description, boolean isWarning, UUID createdBy) {
        LabelCategoryEntity entity = new LabelCategoryEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setWarning(isWarning);
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(Instant.now());
        Session s = session();
        s.persist(entity);
        s.flush();
        return entity.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelDto> listLabels(UUID categoryId, String query) {
        StringBuilder hql = new StringBuilder(
                """
                SELECT l.id, l.categoryId, c.name, l.value, l.description,
                       l.warning, l.createdBy, l.createdAt, l.updatedAt,
                       (SELECT COUNT(a) FROM LabelAssignmentEntity a WHERE a.labelId = l.id)
                FROM LabelEntity l, LabelCategoryEntity c
                WHERE c.id = l.categoryId
                """
        );
        if (categoryId != null) {
            hql.append(" AND l.categoryId = :categoryId");
        }
        if (query != null && !query.isBlank()) {
            hql.append(" AND (lower(l.value) LIKE lower(:pattern)"
                    + " OR lower(COALESCE(l.description, '')) LIKE lower(:pattern))");
        }
        hql.append(" ORDER BY l.warning DESC, lower(c.name) ASC, lower(l.value) ASC");

        var q = session().createSelectionQuery(hql.toString(), Object[].class);
        if (categoryId != null) {
            q.setParameter("categoryId", categoryId);
        }
        if (query != null && !query.isBlank()) {
            q.setParameter("pattern", "%" + query.trim() + "%");
        }
        return mapLabelRows(q.getResultList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LabelDto> findLabelById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        List<Object[]> rows = session().createSelectionQuery(
                """
                SELECT l.id, l.categoryId, c.name, l.value, l.description,
                       l.warning, l.createdBy, l.createdAt, l.updatedAt,
                       (SELECT COUNT(a) FROM LabelAssignmentEntity a WHERE a.labelId = l.id)
                FROM LabelEntity l, LabelCategoryEntity c
                WHERE c.id = l.categoryId AND l.id = :id
                """,
                Object[].class
        )
                .setParameter("id", id)
                .getResultList();
        return mapLabelRows(rows).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsLabelInCategory(UUID categoryId, String value, UUID excludeLabelId) {
        StringBuilder hql = new StringBuilder(
                "SELECT COUNT(l) FROM LabelEntity l WHERE l.categoryId = :categoryId "
                        + "AND lower(l.value) = lower(:value)"
        );
        if (excludeLabelId != null) {
            hql.append(" AND l.id <> :excludeLabelId");
        }
        var q = session().createSelectionQuery(hql.toString(), Long.class)
                .setParameter("categoryId", categoryId)
                .setParameter("value", value);
        if (excludeLabelId != null) {
            q.setParameter("excludeLabelId", excludeLabelId);
        }
        Long count = q.uniqueResult();
        return count != null && count > 0;
    }

    @Override
    public UUID insertLabel(
            UUID categoryId,
            String value,
            String description,
            boolean isWarning,
            UUID createdBy
    ) {
        Instant now = Instant.now();
        LabelEntity entity = new LabelEntity();
        entity.setCategoryId(categoryId);
        entity.setValue(value);
        entity.setDescription(description);
        entity.setWarning(isWarning);
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        Session s = session();
        s.persist(entity);
        s.flush();
        return entity.getId();
    }

    @Override
    public void updateLabel(UUID id, String value, String description, boolean isWarning) {
        session().createMutationQuery(
                "UPDATE LabelEntity l SET l.value = :value, l.description = :description, "
                        + "l.warning = :warning, l.updatedAt = :now WHERE l.id = :id"
        )
                .setParameter("value", value)
                .setParameter("description", description)
                .setParameter("warning", isWarning)
                .setParameter("now", Instant.now())
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public int countAssignments(UUID labelId) {
        Long count = session()
                .createSelectionQuery(
                        "SELECT COUNT(a) FROM LabelAssignmentEntity a WHERE a.labelId = :labelId",
                        Long.class
                )
                .setParameter("labelId", labelId)
                .uniqueResult();
        return count != null ? count.intValue() : 0;
    }

    @Override
    public void deleteLabel(UUID id) {
        session().createMutationQuery("DELETE FROM LabelEntity l WHERE l.id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public int assignLabel(UUID labelId, String targetType, UUID targetId, UUID assignedBy) {
        Session s = session();
        Long existing = s.createSelectionQuery(
                "SELECT COUNT(a) FROM LabelAssignmentEntity a "
                        + "WHERE a.labelId = :labelId AND a.targetType = :targetType AND a.targetId = :targetId",
                Long.class
        )
                .setParameter("labelId", labelId)
                .setParameter("targetType", targetType)
                .setParameter("targetId", targetId)
                .uniqueResult();
        if (existing != null && existing > 0) {
            return 0;
        }
        LabelAssignmentEntity assignment = new LabelAssignmentEntity();
        assignment.setLabelId(labelId);
        assignment.setTargetType(targetType);
        assignment.setTargetId(targetId);
        assignment.setAssignedBy(assignedBy);
        assignment.setAssignedAt(Instant.now());
        s.persist(assignment);
        s.flush();
        return 1;
    }

    @Override
    public int unassignLabel(UUID labelId, String targetType, UUID targetId) {
        return session()
                .createMutationQuery(
                        "DELETE FROM LabelAssignmentEntity a "
                                + "WHERE a.labelId = :labelId AND a.targetType = :targetType AND a.targetId = :targetId"
                )
                .setParameter("labelId", labelId)
                .setParameter("targetType", targetType)
                .setParameter("targetId", targetId)
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelDto> listLabelsForTarget(String targetType, UUID targetId) {
        List<Object[]> rows = session().createSelectionQuery(
                """
                SELECT l.id, l.categoryId, c.name, l.value, l.description,
                       l.warning, l.createdBy, l.createdAt, l.updatedAt,
                       (SELECT COUNT(a2) FROM LabelAssignmentEntity a2 WHERE a2.labelId = l.id)
                FROM LabelAssignmentEntity a, LabelEntity l, LabelCategoryEntity c
                WHERE l.id = a.labelId
                  AND c.id = l.categoryId
                  AND a.targetType = :targetType
                  AND a.targetId = :targetId
                ORDER BY l.warning DESC, lower(c.name) ASC, lower(l.value) ASC
                """,
                Object[].class
        )
                .setParameter("targetType", targetType)
                .setParameter("targetId", targetId)
                .getResultList();
        return mapLabelRows(rows);
    }

    private static LabelCategoryDto toCategoryDto(LabelCategoryEntity entity) {
        return new LabelCategoryDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.isWarning(),
                entity.getCreatedBy(),
                entity.getCreatedAt()
        );
    }

    private static List<LabelDto> mapLabelRows(List<Object[]> rows) {
        List<LabelDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new LabelDto(
                    HibernateSessionUtil.toUuid(row[0]),
                    HibernateSessionUtil.toUuid(row[1]),
                    row[2] != null ? String.valueOf(row[2]) : null,
                    row[3] != null ? String.valueOf(row[3]) : null,
                    row[4] != null ? String.valueOf(row[4]) : null,
                    HibernateSessionUtil.toBoolean(row[5]),
                    HibernateSessionUtil.toInt(row[9]),
                    HibernateSessionUtil.toUuid(row[6]),
                    HibernateSessionUtil.toInstant(row[7]),
                    HibernateSessionUtil.toInstant(row[8])
            ));
        }
        return result;
    }
}
