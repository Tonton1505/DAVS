package vn.edu.davs.backend.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.davs.backend.model.Dashboard;

@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, Long> {
}
