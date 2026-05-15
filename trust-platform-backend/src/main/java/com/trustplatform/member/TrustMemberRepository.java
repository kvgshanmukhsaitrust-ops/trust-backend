package com.trustplatform.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TrustMemberRepository extends JpaRepository<TrustMember, Long> {
    List<TrustMember> findAllByOrderByDisplayOrderAsc();
    List<TrustMember> findByPublishedTrueOrderByDisplayOrderAsc();
}