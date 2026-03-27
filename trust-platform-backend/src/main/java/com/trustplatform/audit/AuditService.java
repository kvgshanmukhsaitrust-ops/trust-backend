package com.trustplatform.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(String action, String performedBy, String details) {
        AuditLog entry = AuditLog.builder()
                .action(action)
                .performedBy(performedBy)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(entry);
    }
    public List<AuditLog> getRecentActivities() {
        // Fetches the last 20 activities sorted by newest first
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }
}