package vn.edu.davs.backend.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.davs.backend.model.Parameter;

@Repository
public interface ParameterRepository extends JpaRepository<Parameter, Long> {
}
