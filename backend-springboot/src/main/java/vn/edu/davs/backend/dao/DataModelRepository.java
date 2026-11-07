package vn.edu.davs.backend.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.davs.backend.model.DataModelRelation;

@Repository
public interface DataModelRepository extends JpaRepository<DataModelRelation, Long> {
}
