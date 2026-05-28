package com.trustplatform.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Async persist rich audit context to prevent thread blocking during transactions
     */
    @Async
    @Transactional
    public void log(String action, String performedBy, String targetResource, String details, 
                    String ipAddress, String userAgent, String status, String errorMessage) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .performedBy(performedBy != null ? performedBy : "anonymous")
                    .targetResource(targetResource)
                    .details(details)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .status(status)
                    .errorMessage(errorMessage)
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to persist async audit log for action: {}", action, e);
        }
    }

    /**
     * Overloaded async log supporting existing 7-parameter signature for compatibility
     */
    @Async
    @Transactional
    public void log(String action, String performedBy, String details, 
                    String ipAddress, String userAgent, String status, String errorMessage) {
        log(action, performedBy, null, details, ipAddress, userAgent, status, errorMessage);
    }

    /**
     * Standard synchronous fallback log for manual logging back-compatibility
     */
    @Transactional
    public void log(String action, String performedBy, String details) {
        log(action, performedBy, null, details, "127.0.0.1", "System-Fallback", "SUCCESS", null);
    }

    /**
     * Paginated search for Audit panel listing
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> searchLogs(String search, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return auditLogRepository.searchLogs(search, status, pageable);
    }
}