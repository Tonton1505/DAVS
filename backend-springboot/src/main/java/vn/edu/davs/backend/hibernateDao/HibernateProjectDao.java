package vn.edu.davs.backend.hibernateDao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.davs.backend.common.Member2Constants;
import vn.edu.davs.backend.dao.ProjectDao;
import vn.edu.davs.backend.dto.CollectionDto;
import vn.edu.davs.backend.dto.ProjectDto;
import vn.edu.davs.backend.entity.CollectionEntity;
import vn.edu.davs.backend.entity.ProjectEntity;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.util.HibernateSessionUtil;

/**
 * Hibernate Session implementation of {@link ProjectDao} for UC05 &amp; UC06.
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class HibernateProjectDao implements ProjectDao {

    @PersistenceContext
    private EntityManager entityManager;

    private Session session() {
        return HibernateSessionUtil.currentSession(entityManager);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDto> listActiveProjects() {
        List<ProjectEntity> entities = session()
                .createSelectionQuery(
                        "FROM ProjectEntity p WHERE p.deletedAt IS NULL "
                                + "ORDER BY p.defaultProject DESC, lower(p.name) ASC",
                        ProjectEntity.class
                )
                .getResultList();
        return entities.stream()
                .map(entity -> toDto(entity, computeDepth(entity.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectDto> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return session()
                .createSelectionQuery(
                        "FROM ProjectEntity p WHERE p.id = :id AND p.deletedAt IS NULL",
                        ProjectEntity.class
                )
                .setParameter("id", id)
                .uniqueResultOptional()
                .map(entity -> toDto(entity, computeDepth(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeletedProjectRow> findDeletedById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return session()
                .createSelectionQuery(
                        "FROM ProjectEntity p WHERE p.id = :id AND p.deletedAt IS NOT NULL",
                        ProjectEntity.class
                )
                .setParameter("id", id)
                .uniqueResultOptional()
                .map(entity -> new DeletedProjectRow(toDto(entity, 1), entity.getDeletedAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsSiblingName(UUID parentId, String name) {
        return existsSiblingNameExcluding(parentId, name, null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsSiblingNameExcluding(UUID parentId, String name, UUID excludeId) {
        StringBuilder hql = new StringBuilder(
                "SELECT COUNT(p) FROM ProjectEntity p WHERE p.deletedAt IS NULL AND lower(p.name) = lower(:name)"
        );
        if (parentId == null) {
            hql.append(" AND p.parentId IS NULL");
        } else {
            hql.append(" AND p.parentId = :parentId");
        }
        if (excludeId != null) {
            hql.append(" AND p.id <> :excludeId");
        }

        var query = session().createSelectionQuery(hql.toString(), Long.class)
                .setParameter("name", name);
        if (parentId != null) {
            query.setParameter("parentId", parentId);
        }
        if (excludeId != null) {
            query.setParameter("excludeId", excludeId);
        }
        Long count = query.uniqueResult();
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public int computeDepth(UUID projectId) {
        int depth = 0;
        UUID current = projectId;
        Set<UUID> visited = new HashSet<>();
        Session s = session();
        while (current != null && visited.add(current)) {
            depth++;
            List<UUID> parents = s.createSelectionQuery(
                    "SELECT p.parentId FROM ProjectEntity p WHERE p.id = :id AND p.deletedAt IS NULL",
                    UUID.class
            )
                    .setParameter("id", current)
                    .getResultList();
            if (parents.isEmpty()) {
                return 0;
            }
            current = parents.get(0);
        }
        return depth;
    }

    @Override
    @Transactional(readOnly = true)
    public int computeSubtreeHeight(UUID projectId) {
        List<UUID> children = session()
                .createSelectionQuery(
                        "SELECT p.id FROM ProjectEntity p WHERE p.parentId = :id AND p.deletedAt IS NULL",
                        UUID.class
                )
                .setParameter("id", projectId)
                .getResultList();
        int maxChild = 0;
        for (UUID childId : children) {
            maxChild = Math.max(maxChild, computeSubtreeHeight(childId));
        }
        return 1 + maxChild;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDescendantOf(UUID candidateId, UUID ancestorId) {
        if (candidateId == null || ancestorId == null) {
            return false;
        }
        UUID current = candidateId;
        Set<UUID> visited = new HashSet<>();
        Session s = session();
        while (current != null && visited.add(current)) {
            if (current.equals(ancestorId)) {
                return true;
            }
            List<UUID> parents = s.createSelectionQuery(
                    "SELECT p.parentId FROM ProjectEntity p WHERE p.id = :id AND p.deletedAt IS NULL",
                    UUID.class
            )
                    .setParameter("id", current)
                    .getResultList();
            if (parents.isEmpty()) {
                return false;
            }
            current = parents.get(0);
        }
        return false;
    }

    @Override
    public UUID insertProject(
            String name,
            String description,
            UUID parentId,
            UUID ownerId,
            String permissionMode
    ) {
        Instant now = Instant.now();
        ProjectEntity entity = new ProjectEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setParentId(parentId);
        entity.setOwnerId(ownerId);
        entity.setPermissionMode(permissionMode);
        entity.setDefaultProject(false);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        Session s = session();
        s.persist(entity);
        s.flush();
        return entity.getId();
    }

    @Override
    public void updateProject(UUID id, String name, String description) {
        session().createMutationQuery(
                "UPDATE ProjectEntity p SET p.name = :name, p.description = :description, "
                        + "p.updatedAt = :now WHERE p.id = :id AND p.deletedAt IS NULL"
        )
                .setParameter("name", name)
                .setParameter("description", description)
                .setParameter("now", Instant.now())
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public void moveProject(UUID id, UUID targetParentId) {
        session().createMutationQuery(
                "UPDATE ProjectEntity p SET p.parentId = :parentId, "
                        + "p.updatedAt = :now WHERE p.id = :id AND p.deletedAt IS NULL"
        )
                .setParameter("parentId", targetParentId)
                .setParameter("now", Instant.now())
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public void updateOwner(UUID id, UUID newOwnerId) {
        session().createMutationQuery(
                "UPDATE ProjectEntity p SET p.ownerId = :ownerId, "
                        + "p.updatedAt = :now WHERE p.id = :id AND p.deletedAt IS NULL"
        )
                .setParameter("ownerId", newOwnerId)
                .setParameter("now", Instant.now())
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public void softDeleteProject(UUID id) {
        Session s = session();
        s.createNativeMutationQuery(
                """
                WITH RECURSIVE subtree AS (
                    SELECT id FROM projects WHERE id = :id
                    UNION ALL
                    SELECT p.id FROM projects p INNER JOIN subtree s ON p.parent_id = s.id
                )
                UPDATE projects
                SET deleted_at = now(), updated_at = now()
                WHERE id IN (SELECT id FROM subtree) AND deleted_at IS NULL
                """
        )
                .setParameter("id", id)
                .executeUpdate();

        s.createNativeMutationQuery(
                """
                UPDATE workbooks
                SET deleted_at = now(), updated_at = now()
                WHERE project_id = :id AND deleted_at IS NULL
                """
        )
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public void restoreProject(UUID id) {
        Session s = session();
        s.createNativeMutationQuery(
                """
                WITH RECURSIVE subtree AS (
                    SELECT id FROM projects WHERE id = :id
                    UNION ALL
                    SELECT p.id FROM projects p INNER JOIN subtree s ON p.parent_id = s.id
                )
                UPDATE projects
                SET deleted_at = NULL, updated_at = now()
                WHERE id IN (SELECT id FROM subtree)
                """
        )
                .setParameter("id", id)
                .executeUpdate();

        s.createNativeMutationQuery(
                """
                UPDATE workbooks
                SET deleted_at = NULL, updated_at = now()
                WHERE project_id = :id
                """
        )
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public void copyParentPermissionRules(UUID parentId, UUID childId) {
        session().createNativeMutationQuery(
                """
                INSERT INTO permission_rules (
                    resource_type, resource_id, subject_type, subject_id, capability, effect, created_by
                )
                SELECT 'PROJECT', :childId, subject_type, subject_id, capability, effect, created_by
                FROM permission_rules
                WHERE resource_type = 'PROJECT' AND resource_id = :parentId
                ON CONFLICT DO NOTHING
                """
        )
                .setParameter("childId", childId)
                .setParameter("parentId", parentId)
                .executeUpdate();
    }

    @Override
    public void replaceProjectRulesFromParent(UUID parentId, UUID childId) {
        session().createMutationQuery(
                "DELETE FROM PermissionRuleEntity r WHERE r.resourceType = :resType AND r.resourceId = :childId"
        )
                .setParameter("resType", Member2Constants.RESOURCE_PROJECT)
                .setParameter("childId", childId)
                .executeUpdate();
        copyParentPermissionRules(parentId, childId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OwnerCandidateRow> findUserForOwnership(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        List<Object[]> rows = session().createNativeQuery(
                """
                SELECT id, email, COALESCE(NULLIF(full_name, ''), email) AS full_name, site_role, status
                FROM users
                WHERE id = :userId AND deleted_at IS NULL
                """,
                Object[].class
        )
                .setParameter("userId", userId)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        return Optional.of(new OwnerCandidateRow(
                HibernateSessionUtil.toUuid(row[0]),
                String.valueOf(row[1]),
                String.valueOf(row[2]),
                SiteRole.valueOf(String.valueOf(row[3])),
                String.valueOf(row[4])
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public String findOwnerDisplayName(UUID ownerId) {
        if (ownerId == null) {
            return Member2Constants.DEFAULT_SYSTEM_OWNER_NAME;
        }
        List<String> names = session().createNativeQuery(
                "SELECT COALESCE(NULLIF(full_name, ''), email) FROM users WHERE id = :ownerId",
                String.class
        )
                .setParameter("ownerId", ownerId)
                .getResultList();
        return names.isEmpty() ? Member2Constants.DEFAULT_SYSTEM_OWNER_NAME : names.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public int countChildItems(UUID projectId) {
        Session s = session();
        Long subProjects = s.createSelectionQuery(
                "SELECT COUNT(p) FROM ProjectEntity p WHERE p.parentId = :projectId AND p.deletedAt IS NULL",
                Long.class
        )
                .setParameter("projectId", projectId)
                .uniqueResult();
        Number workbooks = s.createNativeQuery(
                "SELECT COUNT(*) FROM workbooks WHERE project_id = :projectId AND deleted_at IS NULL",
                Number.class
        )
                .setParameter("projectId", projectId)
                .getSingleResult();
        int subCount = subProjects != null ? subProjects.intValue() : 0;
        int wbCount = workbooks != null ? workbooks.intValue() : 0;
        return subCount + wbCount;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollectionDto> listCollectionsByOwner(UUID ownerId) {
        List<CollectionEntity> collections = session()
                .createSelectionQuery(
                        "FROM CollectionEntity c WHERE c.ownerId = :ownerId "
                                + "ORDER BY c.updatedAt DESC, lower(c.name) ASC",
                        CollectionEntity.class
                )
                .setParameter("ownerId", ownerId)
                .getResultList();
        return collections.stream().map(this::toCollectionDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CollectionDto> findCollectionById(UUID collectionId) {
        if (collectionId == null) {
            return Optional.empty();
        }
        return session()
                .createSelectionQuery(
                        "FROM CollectionEntity c WHERE c.id = :id",
                        CollectionEntity.class
                )
                .setParameter("id", collectionId)
                .uniqueResultOptional()
                .map(this::toCollectionDto);
    }

    @Override
    public UUID insertCollection(String name, String description, UUID ownerId) {
        Instant now = Instant.now();
        CollectionEntity entity = new CollectionEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setOwnerId(ownerId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        Session s = session();
        s.persist(entity);
        s.flush();
        return entity.getId();
    }

    @Override
    public void addCollectionItem(UUID collectionId, String resourceType, UUID resourceId) {
        Session s = session();
        s.createNativeMutationQuery(
                """
                INSERT INTO collection_items (collection_id, resource_type, resource_id)
                VALUES (:collectionId, :resourceType, :resourceId)
                ON CONFLICT (collection_id, resource_type, resource_id) DO NOTHING
                """
        )
                .setParameter("collectionId", collectionId)
                .setParameter("resourceType", resourceType)
                .setParameter("resourceId", resourceId)
                .executeUpdate();

        s.createMutationQuery(
                "UPDATE CollectionEntity c SET c.updatedAt = :now WHERE c.id = :collectionId"
        )
                .setParameter("now", Instant.now())
                .setParameter("collectionId", collectionId)
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollectionDto.CollectionItemDto> listCollectionItems(UUID collectionId) {
        List<Object[]> rows = session().createNativeQuery(
                """
                SELECT ci.collection_id, ci.resource_type, ci.resource_id, ci.added_at,
                       COALESCE(p.name, w.name, ci.resource_id::text) AS resource_name
                FROM collection_items ci
                LEFT JOIN projects p ON ci.resource_type = 'PROJECT' AND p.id = ci.resource_id
                LEFT JOIN workbooks w ON ci.resource_type = 'WORKBOOK' AND w.id = ci.resource_id
                WHERE ci.collection_id = :collectionId
                ORDER BY ci.added_at DESC
                """,
                Object[].class
        )
                .setParameter("collectionId", collectionId)
                .getResultList();
        List<CollectionDto.CollectionItemDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new CollectionDto.CollectionItemDto(
                    HibernateSessionUtil.toUuid(row[0]),
                    String.valueOf(row[1]),
                    HibernateSessionUtil.toUuid(row[2]),
                    String.valueOf(row[4]),
                    HibernateSessionUtil.toInstant(row[3])
            ));
        }
        return result;
    }

    private CollectionDto toCollectionDto(CollectionEntity entity) {
        List<CollectionDto.CollectionItemDto> items = listCollectionItems(entity.getId());
        return new CollectionDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getOwnerId(),
                items.size(),
                items,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ProjectDto toDto(ProjectEntity entity, int depth) {
        return new ProjectDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getParentId(),
                entity.getOwnerId(),
                entity.getPermissionMode(),
                entity.isDefaultProject(),
                depth,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
