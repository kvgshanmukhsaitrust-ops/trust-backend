package com.trustplatform.notification;

import com.trustplatform.common.api.ApiSuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_CONTENT')")
    public ResponseEntity<ApiSuccessResponse<Page<Notification>>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        
        Page<Notification> alerts = notificationService.getNotifications(
                userDetails.getUsername(), category, isRead, search, page, size);
        return ResponseEntity.ok(
                ApiSuccessResponse.<Page<Notification>>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Notifications fetched successfully")
                        .data(alerts)
                        .build()
        );
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('READ_CONTENT')")
    public ResponseEntity<ApiSuccessResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        long count = notificationService.getUnreadCount(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiSuccessResponse.<Long>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Unread notification count fetched successfully")
                        .data(count)
                        .build()
        );
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAuthority('READ_CONTENT')")
    public ResponseEntity<ApiSuccessResponse<Void>> markAsRead(
            @PathVariable Long id) {
        
        notificationService.markAsRead(id);
        return ResponseEntity.ok(
                ApiSuccessResponse.<Void>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Notification marked as read")
                        .build()
        );
    }

    @PostMapping("/read-all")
    @PreAuthorize("hasAuthority('READ_CONTENT')")
    public ResponseEntity<ApiSuccessResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        notificationService.markAllAsRead(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiSuccessResponse.<Void>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("All notifications marked as read")
                        .build()
        );
    }
}
