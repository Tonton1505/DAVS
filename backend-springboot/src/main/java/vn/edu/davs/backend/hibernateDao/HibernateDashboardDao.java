package vn.edu.davs.backend.hibernateDao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.davs.backend.common.Member2Constants;
import vn.edu.davs.backend.dao.DashboardDao;
import vn.edu.davs.backend.dto.DashboardDto;
import vn.edu.davs.backend.entity.DashboardEntity;
import vn.edu.davs.backend.util.HibernateSessionUtil;

/**
 * Hibernate Session implementation of {@link DashboardDao} for UC17 &amp; UC19.
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class HibernateDashboardDao implements DashboardDao {

    @PersistenceContext
    private EntityManager entityManager;

    private Session session() {
        return HibernateSessionUtil.currentSession(entityManager);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DashboardDto> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return session()
                .createSelectionQuery(
                        "FROM DashboardEntity d WHERE d.id = :id AND d.deletedAt IS NULL",
                        DashboardEntity.class
                )
                .setParameter("id", id)
                .uniqueResultOptional()
                .map(HibernateDashboardDao::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardDto> findByWorkbookId(UUID workbookId) {
        return session()
                .createSelectionQuery(
                        "FROM DashboardEntity d WHERE d.workbookId = :workbookId "
                                + "AND d.deletedAt IS NULL ORDER BY d.createdAt ASC",
                        DashboardEntity.class
                )
                .setParameter("workbookId", workbookId)
                .getResultList()
                .stream()
                .map(HibernateDashboardDao::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DashboardDto> findFirstActive() {
        return session()
                .createSelectionQuery(
                        "FROM DashboardEntity d WHERE d.deletedAt IS NULL ORDER BY d.createdAt ASC",
                        DashboardEntity.class
                )
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst()
                .map(HibernateDashboardDao::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardDto> listAllActive() {
        return session()
                .createSelectionQuery(
                        "FROM DashboardEntity d WHERE d.deletedAt IS NULL ORDER BY d.updatedAt DESC",
                        DashboardEntity.class
                )
                .getResultList()
                .stream()
                .map(HibernateDashboardDao::toDto)
                .toList();
    }

    @Override
    public DashboardDto create(
            UUID workbookId,
            String name,
            String description,
            String definition,
            UUID createdBy
    ) {
        Instant now = Instant.now();
        String jsonDef = (definition != null && !definition.isBlank())
                ? definition
                : Member2Constants.EMPTY_JSON_OBJECT;

        DashboardEntity entity = new DashboardEntity();
        entity.setWorkbookId(workbookId);
        entity.setName(name);
        entity.setDescription(description);
        entity.setSizeType("FIXED");
        entity.setWidth(1366);
        entity.setHeight(768);
        entity.setTiles(Member2Constants.EMPTY_JSON_ARRAY);
        entity.setActions(Member2Constants.EMPTY_JSON_ARRAY);
        entity.setDefinition(jsonDef);
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        Session s = session();
        s.persist(entity);
        s.flush();
        return toDto(entity);
    }

    @Override
    public void update(UUID id, String name, String description, String definition) {
        Session s = session();
        DashboardEntity entity = s.find(DashboardEntity.class, id);
        if (entity == null || entity.getDeletedAt() != null) {
            return;
        }
        if (name != null) {
            entity.setName(name);
        }
        if (description != null) {
            entity.setDescription(description);
        }
        if (definition != null) {
            entity.setDefinition(definition);
        }
        entity.setUpdatedAt(Instant.now());
        s.flush();
    }

    @Override
    public void delete(UUID id) {
        session().createMutationQuery("UPDATE DashboardEntity d SET d.deletedAt = :now WHERE d.id = :id")
                .setParameter("now", Instant.now())
                .setParameter("id", id)
                .executeUpdate();
    }

    private static DashboardDto toDto(DashboardEntity entity) {
        return new DashboardDto(
                entity.getId(),
                entity.getWorkbookId(),
                entity.getName(),
                entity.getDescription(),
                entity.getDefinition(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
