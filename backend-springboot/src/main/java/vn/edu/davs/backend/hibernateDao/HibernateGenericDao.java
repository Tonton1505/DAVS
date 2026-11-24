package vn.edu.davs.backend.hibernateDao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.davs.backend.common.MessageConstants;
import vn.edu.davs.backend.dao.IGenericDao;

/**
 * Base Hibernate Generic DAO providing standard CRUD and basic JPQL operations.
 */
public class HibernateGenericDao<Pk, Entity> extends SimpleJpaRepository<Entity, Pk>
        implements IGenericDao<Pk, Entity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateGenericDao.class);
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z0-9_.]+$");

    private final Class<Entity> type;
    private final EntityManager entityManager;

    public HibernateGenericDao(Class<Entity> type, EntityManager entityManager) {
        super(type, entityManager);
        this.type = type;
        this.entityManager = entityManager;
    }

    public Class<Entity> getType() {
        return type;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected boolean isValidIdentifier(String identifier) {
        return identifier != null && SAFE_IDENTIFIER.matcher(identifier).matches();
    }

    @Override
    @Transactional
    public Entity create(Entity anEntity) {
        if (anEntity == null) {
            throw new IllegalArgumentException(
                    String.format(MessageConstants.CANNOT_CREATE_NULL_ENTITY, type.getSimpleName()));
        }
        entityManager.persist(anEntity);
        return anEntity;
    }

    @Override
    @Transactional
    public Entity update(Entity anEntity) {
        if (anEntity == null) {
            throw new IllegalArgumentException(
                    String.format(MessageConstants.CANNOT_UPDATE_NULL_ENTITY, type.getSimpleName()));
        }
        return entityManager.merge(anEntity);
    }

    @Override
    @Transactional
    public Entity saveOrUpdate(Entity anEntity) {
        if (anEntity == null) {
            throw new IllegalArgumentException(
                    String.format(MessageConstants.CANNOT_SAVE_OR_UPDATE_NULL_ENTITY, type.getSimpleName()));
        }
        return save(anEntity);
    }

    @Override
    @Transactional
    public void delete(Entity anEntity) {
        if (anEntity == null) {
            throw new IllegalArgumentException(
                    String.format(MessageConstants.CANNOT_DELETE_NULL_ENTITY, type.getSimpleName()));
        }
        super.delete(anEntity);
    }

    @Override
    @Transactional
    public void deleteById(Pk id) {
        if (id == null) {
            throw new IllegalArgumentException(
                    String.format(MessageConstants.CANNOT_DELETE_NULL_ID, type.getSimpleName()));
        }
        super.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByPk(Pk id) {
        deleteById(id);
    }

    @Override
    public Entity get(Pk id) {
        return findById(id).orElse(null);
    }

    @Override
    public Entity findByPk(Pk id) {
        return get(id);
    }

    @Override
    public Optional<Entity> findById(Pk id) {
        if (id == null) {
            return Optional.empty();
        }
        return super.findById(id);
    }

    @Override
    public Entity getByField(String fieldName, Object value) {
        if (!isValidIdentifier(fieldName)) {
            return null;
        }
        try {
            String jpql = "SELECT e FROM " + type.getSimpleName() + " e WHERE e." + fieldName + " = :value";
            TypedQuery<Entity> query = entityManager.createQuery(jpql, type);
            query.setParameter("value", value);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<Entity> findByField(String fieldName, Object value) {
        if (!isValidIdentifier(fieldName)) {
            return Collections.emptyList();
        }
        String jpql = "SELECT e FROM " + type.getSimpleName() + " e WHERE e." + fieldName + " = :value";
        TypedQuery<Entity> query = entityManager.createQuery(jpql, type);
        query.setParameter("value", value);
        return query.getResultList();
    }

    @Override
    public List<Entity> findByHql(String hql, Map<String, Object> params) {
        TypedQuery<Entity> query = entityManager.createQuery(hql, type);
        if (params != null) {
            params.forEach(query::setParameter);
        }
        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Entity> findByNativeQuery(String sql, Map<String, Object> params) {
        Query query = entityManager.createNativeQuery(sql, type);
        if (params != null) {
            params.forEach(query::setParameter);
        }
        return query.getResultList();
    }

    @Override
    public Long countAll() {
        LOGGER.debug("Counting all {}", type.getSimpleName());
        return count();
    }

    @Override
    public boolean exists(Pk id) {
        return id != null && existsById(id);
    }
}
