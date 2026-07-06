package com.trustplatform.applicant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssistanceCaseRepository extends JpaRepository<AssistanceCase, Long> {

    @EntityGraph(attributePaths = {"applicant", "assignedOfficer"})
    Optional<AssistanceCase> findByCaseNumber(String caseNumber);

    @EntityGraph(attributePaths = {"applicant", "assignedOfficer"})
    Page<AssistanceCase> findByApplicantId(Long applicantId, Pageable pageable);

    @EntityGraph(attributePaths = {"applicant", "assignedOfficer"})
    Page<AssistanceCase> findByStatus(CaseStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"applicant", "assignedOfficer"})
    Page<AssistanceCase> findByCategory(CaseCategory category, Pageable pageable);
}
