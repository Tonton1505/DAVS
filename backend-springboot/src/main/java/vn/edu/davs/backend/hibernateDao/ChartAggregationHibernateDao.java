package vn.edu.davs.backend.hibernateDao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class ChartAggregationHibernateDao {
    @PersistenceContext
    private EntityManager entityManager;
}
