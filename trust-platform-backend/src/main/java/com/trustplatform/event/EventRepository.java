package com.trustplatform.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByDeletedFalse(Pageable pageable);

    Page<Event> findByStatusAndDeletedFalse(EventStatus status, Pageable pageable);

    Page<Event> findByEventDateBeforeAndDeletedFalse(LocalDateTime now, Pageable pageable);
    long countByStatus(EventStatus status);
 

  
}