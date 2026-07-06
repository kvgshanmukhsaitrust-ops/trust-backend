package com.trustplatform.applicant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseMilestoneRepository extends JpaRepository<CaseMilestone, Long> {
    List<CaseMilestone> findByAssistanceCaseIdOrderByTimestampAsc(Long caseId);
}
