package com.trustplatform.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentVersionRepository extends JpaRepository<ContentVersion, Long> {
    List<ContentVersion> findByEntityTypeAndEntityIdOrderByVersionNumberDesc(String entityType, String entityId);
    
    ContentVersion findTopByEntityTypeAndEntityIdOrderByVersionNumberDesc(String entityType, String entityId);
}
