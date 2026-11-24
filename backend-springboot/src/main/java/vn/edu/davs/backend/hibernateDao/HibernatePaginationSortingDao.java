package vn.edu.davs.backend.hibernateDao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.davs.backend.dao.IPaginationSortingDao;
import vn.edu.davs.backend.specification.GenericSpecificationsBuilder;

/**
 * Specialized DAO implementation extending {@link HibernateGenericDao} for sorting,
 * pagination, and JPA Specification queries.
 */
public class HibernatePaginationSortingDao<Pk, Entity> extends HibernateGenericDao<Pk, Entity>
        implements IPaginationSortingDao<Pk, Entity> {

    public HibernatePaginationSortingDao(Class<Entity> type, EntityManager entityManager) {
        super(type, entityManager);
    }

    @Override
    public List<Entity> findAllSorted(String sortBy, String direction) {
        return findAll(buildSort(sortBy, direction));
    }

    @Override
    public Page<Entity> findAllPaged(int page, int size, String sortBy, String direction) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), buildSort(sortBy, direction));
        return findAll(pageable);
    }

    @Override
    public List<Entity> findByFieldSorted(String fieldName, Object value, Sort sort) {
        if (!isValidIdentifier(fieldName)) {
            return Collections.emptyList();
        }
        String jpql = "SELECT e FROM " + getType().getSimpleName()
                + " e WHERE e." + fieldName + " = :value" + buildOrderClause(sort);
        TypedQuery<Entity> query = getEntityManager().createQuery(jpql, getType());
        query.setParameter("value", value);
        return query.getResultList();
    }

    @Override
    public List<Entity> findByFieldSorted(String fieldName, Object value, String sortBy, String direction) {
        return findByFieldSorted(fieldName, value, buildSort(sortBy, direction));
    }

    @Override
    public Page<Entity> findByFieldPaged(String fieldName, Object value, Pageable pageable) {
        if (!isValidIdentifier(fieldName)) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        String countJpql = "SELECT COUNT(e) FROM " + getType().getSimpleName() + " e WHERE e." + fieldName + " = :value";
        Long total = getEntityManager().createQuery(countJpql, Long.class)
                .setParameter("value", value)
                .getSingleResult();
        if (total == 0L) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        String jpql = "SELECT e FROM " + getType().getSimpleName()
                + " e WHERE e." + fieldName + " = :value" + buildOrderClause(pageable.getSort());
        TypedQuery<Entity> query = getEntityManager().createQuery(jpql, getType());
        query.setParameter("value", value);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        return new PageImpl<>(query.getResultList(), pageable, total);
    }

    @Override
    public Page<Entity> findByFieldPaged(
            String fieldName, Object value, int page, int size, String sortBy, String direction) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), buildSort(sortBy, direction));
        return findByFieldPaged(fieldName, value, pageable);
    }

    @Override
    public List<Entity> searchBySpecification(String search) {
        Specification<Entity> spec = new GenericSpecificationsBuilder<Entity>().withSearchQuery(search).build();
        return spec != null ? findAll(spec) : findAll();
    }

    @Override
    public List<Entity> searchBySpecificationSorted(String search, String sortBy, String direction) {
        Specification<Entity> spec = new GenericSpecificationsBuilder<Entity>().withSearchQuery(search).build();
        Sort sort = buildSort(sortBy, direction);
        return spec != null ? findAll(spec, sort) : findAll(sort);
    }

    @Override
    public Page<Entity> searchBySpecificationPaged(
            String search, int page, int size, String sortBy, String direction) {
        Specification<Entity> spec = new GenericSpecificationsBuilder<Entity>().withSearchQuery(search).build();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), buildSort(sortBy, direction));
        return spec != null ? findAll(spec, pageable) : findAll(pageable);
    }

    @Override
    public List<Entity> search(
            String query, Integer lowerLimit, Integer upperLimit, String orderBy, String orderType) {
        int offset = lowerLimit != null && lowerLimit >= 0 ? lowerLimit : 0;
        int limit = upperLimit != null && upperLimit > 0 ? upperLimit : 25;
        int page = offset / limit;
        return searchBySpecificationPaged(query, page, limit, orderBy, orderType).getContent();
    }

    protected Sort buildSort(String sortBy, String direction) {
        if (!isValidIdentifier(sortBy)) {
            return Sort.unsorted();
        }
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, sortBy);
    }

    protected String buildOrderClause(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" ORDER BY ");
        boolean first = true;
        for (Sort.Order order : sort) {
            if (!isValidIdentifier(order.getProperty())) {
                continue;
            }
            if (!first) {
                sb.append(", ");
            }
            sb.append("e.").append(order.getProperty()).append(" ").append(order.getDirection().name());
            first = false;
        }
        return first ? "" : sb.toString();
    }
}
