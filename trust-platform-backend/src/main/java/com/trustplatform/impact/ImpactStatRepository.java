package com.trustplatform.impact;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImpactStatRepository extends JpaRepository<ImpactStat, Long> {
    Optional<ImpactStat> findByCategory(String category);
    List<ImpactStat> findAllByOrderByDisplayOrderAsc();
}