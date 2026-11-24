package vn.edu.davs.backend.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base Generic DAO interface defining standard CRUD and basic query operations.
 */
public interface IGenericDao<Pk, Entity> {

    @Transactional
    Entity create(Entity anEntity);

    @Transactional
    Entity update(Entity anEntity);

    @Transactional
    Entity saveOrUpdate(Entity anEntity);

    @Transactional
    void delete(Entity anEntity);

    @Transactional
    void deleteById(Pk id);

    void deleteByPk(Pk id);

    Entity get(Pk id);

    Entity findByPk(Pk id);

    Optional<Entity> findById(Pk id);

    Entity getByField(String fieldName, Object value);

    List<Entity> findByField(String fieldName, Object value);

    List<Entity> findByHql(String hql, Map<String, Object> params);

    List<Entity> findByNativeQuery(String sql, Map<String, Object> params);

    Long countAll();

    boolean exists(Pk id);
}
