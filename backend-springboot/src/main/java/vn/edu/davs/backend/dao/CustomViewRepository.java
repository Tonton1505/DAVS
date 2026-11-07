package vn.edu.davs.backend.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.davs.backend.model.CustomView;

@Repository
public interface CustomViewRepository extends JpaRepository<CustomView, Long> {
}
