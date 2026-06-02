package com.trustplatform.impact;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ImpactShowcaseCardRepository extends JpaRepository<ImpactShowcaseCard, Long> {
    List<ImpactShowcaseCard> findAllByDeletedFalseOrderByDisplayOrderAsc();
}
