package com.trustplatform.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<ContactMessage, Long> {
    
    // Custom query to get the newest messages first
    List<ContactMessage> findAllByOrderByCreatedAtDesc();

    // Custom query for the Admin Dashboard badge
    long countByIsReadFalse();
}