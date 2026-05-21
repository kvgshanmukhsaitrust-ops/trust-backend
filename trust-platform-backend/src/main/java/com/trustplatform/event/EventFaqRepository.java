package com.trustplatform.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EventFaqRepository extends JpaRepository<EventFaq, Long> {
    List<EventFaq> findByEventIdOrderByDisplayOrderAscIdAsc(Long eventId);
    void deleteByEventId(Long eventId);
}
