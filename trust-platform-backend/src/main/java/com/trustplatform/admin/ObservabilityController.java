package com.trustplatform.admin;

import com.trustplatform.common.api.ApiSuccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trustplatform.audit.AuditLogRepository;
import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ObservabilityController {

    private final DataSource dataSource;
    private final AuditLogRepository auditLogRepository;
    private static final long SERVER_START_TIME = System.currentTimeMillis();

    // =========================================
    // PUBLIC HEALTH CHECK (no auth required)
    // =========================================
    @GetMapping("/public/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("service", "trust-platform-backend");
        health.put("version", "2.0.0-enterprise");
        health.put("uptimeSeconds", (System.currentTimeMillis() - SERVER_START_TIME) / 1000);

        // Database connectivity check
        boolean dbOk = false;
        try (Connection conn = dataSource.getConnection()) {
            dbOk = conn.isValid(2);
        } catch (Exception e) {
            log.warn("[ObservabilityController] DB health check failed: {}", e.getMessage());
        }
        health.put("database", dbOk ? "CONNECTED" : "DEGRADED");

        return ResponseEntity.ok(health);
    }

    // =========================================
    // ADMIN METRICS (detailed operational diagnostics)
    // =========================================
    @GetMapping("/admin/metrics")
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> metrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // ── JVM Memory ───────────────────────────────────────────────
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        long heapUsed    = memBean.getHeapMemoryUsage().getUsed()    / (1024 * 1024);
        long heapMax     = memBean.getHeapMemoryUsage().getMax()     / (1024 * 1024);
        long nonHeapUsed = memBean.getNonHeapMemoryUsage().getUsed() / (1024 * 1024);

        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("heapUsedMb",       heapUsed);
        memory.put("heapMaxMb",        heapMax);
        memory.put("heapUsagePercent", heapMax > 0 ? Math.round((double) heapUsed / heapMax * 100) : 0);
        memory.put("nonHeapUsedMb",    nonHeapUsed);
        metrics.put("jvmMemory", memory);

        // ── Thread Stats ─────────────────────────────────────────────
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        Map<String, Object> threads = new LinkedHashMap<>();
        threads.put("activeCount", threadBean.getThreadCount());
        threads.put("peakCount",   threadBean.getPeakThreadCount());
        threads.put("daemonCount", threadBean.getDaemonThreadCount());
        metrics.put("threads", threads);

        // ── Runtime ──────────────────────────────────────────────────
        metrics.put("uptimeSeconds",       (System.currentTimeMillis() - SERVER_START_TIME) / 1000);
        metrics.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        metrics.put("serverTimestamp",     LocalDateTime.now().toString());

        // ── Database Health ──────────────────────────────────────────
        boolean dbOk = false;
        long dbLatencyMs = -1;
        try (Connection conn = dataSource.getConnection()) {
            long start = System.currentTimeMillis();
            dbOk = conn.isValid(2);
            dbLatencyMs = System.currentTimeMillis() - start;
        } catch (Exception e) {
            log.warn("[ObservabilityController] DB metrics check failed: {}", e.getMessage());
        }
        Map<String, Object> db = new LinkedHashMap<>();
        db.put("status",      dbOk ? "CONNECTED" : "DEGRADED");
        db.put("latencyMs",   dbLatencyMs);
        metrics.put("database", db);

        // ── Failed Requests Aggregation ──────────────────────────────
        long failedCount = 0;
        try {
            failedCount = auditLogRepository.countByStatus("FAILED");
        } catch (Exception e) {
            log.warn("[ObservabilityController] Failed to count failed audit logs: {}", e.getMessage());
        }
        metrics.put("failedRequestsCount", failedCount);

        return ResponseEntity.ok(
                ApiSuccessResponse.<Map<String, Object>>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Operational metrics collected")
                        .data(metrics)
                        .build());
    }
}
