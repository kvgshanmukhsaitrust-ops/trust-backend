package com.trustplatform.dashboard;

import com.trustplatform.common.api.ApiSuccessResponse;
import com.trustplatform.dashboard.dto.DashboardSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    public ResponseEntity<ApiSuccessResponse<DashboardSummaryResponse>> getDashboard() {

        DashboardSummaryResponse response = dashboardService.getDashboardSummary();

        return ResponseEntity.ok(
                ApiSuccessResponse.<DashboardSummaryResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Dashboard data fetched successfully")
                        .data(response)
                        .build()
        );
    }
}