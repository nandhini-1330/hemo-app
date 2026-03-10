package com.anemia.repository;

import com.anemia.model.Analysis;
import com.anemia.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    List<Analysis> findByUserOrderByCreatedAtDesc(User user);
    List<Analysis> findTop5ByUserOrderByCreatedAtDesc(User user);
    long countByUser(User user);
    long countByUserAndAnemic(User user, boolean anemic);
}