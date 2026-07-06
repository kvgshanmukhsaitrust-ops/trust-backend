package com.trustplatform.volunteer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

public interface VolunteerRepository extends JpaRepository<VolunteerApplication, Long> {

    Optional<VolunteerApplication> findByUserIdAndEventId(Long userId, Long eventId);
    long countByStatus(VolunteerStatus status);
    List<VolunteerApplication> findByStatus(VolunteerStatus status);
    List<VolunteerApplication> findByUserId(Long userId);
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    List<VolunteerApplication> findByAttendanceVerifiedTrue();
}