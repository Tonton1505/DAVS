package vn.edu.davs.backend.dao;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * Interface extending {@link IGenericDao} dedicated to Sorting, Pagination,
 * and Dynamic Specification Search.
 */
public interface IPaginationSortingDao<Pk, Entity> extends IGenericDao<Pk, Entity> {

    List<Entity> findAllSorted(String sortBy, String direction);

    Page<Entity> findAllPaged(int page, int size, String sortBy, String direction);

    List<Entity> findByFieldSorted(String fieldName, Object value, Sort sort);

    List<Entity> findByFieldSorted(String fieldName, Object value, String sortBy, String direction);

    Page<Entity> findByFieldPaged(String fieldName, Object value, Pageable pageable);

    Page<Entity> findByFieldPaged(String fieldName, Object value, int page, int size, String sortBy, String direction);

    List<Entity> searchBySpecification(String search);

    List<Entity> searchBySpecificationSorted(String search, String sortBy, String direction);

    Page<Entity> searchBySpecificationPaged(String search, int page, int size, String sortBy, String direction);

    List<Entity> search(String query, Integer lowerLimit, Integer upperLimit, String orderBy, String orderType);

    List<Entity> findAll(Specification<Entity> spec);

    Page<Entity> findAll(Specification<Entity> spec, Pageable pageable);
}
