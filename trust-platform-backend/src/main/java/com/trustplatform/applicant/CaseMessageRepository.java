package com.trustplatform.applicant;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseMessageRepository extends JpaRepository<CaseMessage, Long> {

    @EntityGraph(attributePaths = {"sender"})
    List<CaseMessage> findByAssistanceCaseIdOrderBySentAtAsc(Long caseId);

    @EntityGraph(attributePaths = {"sender"})
    List<CaseMessage> findByAssistanceCaseIdAndIsInternalFalseOrderBySentAtAsc(Long caseId);
}
