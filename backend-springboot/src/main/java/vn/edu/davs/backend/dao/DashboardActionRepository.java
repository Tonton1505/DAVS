package vn.edu.davs.backend.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.davs.backend.model.DashboardAction;

@Repository
public interface DashboardActionRepository extends JpaRepository<DashboardAction, Long> {
}
