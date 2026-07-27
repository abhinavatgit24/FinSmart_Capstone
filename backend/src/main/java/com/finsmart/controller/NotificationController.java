package com.finsmart.controller;

import com.finsmart.dto.response.ApiResponse;
import com.finsmart.model.Notification;
import com.finsmart.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** GET /api/notifications — all notifications (refreshes alerts first) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getAll(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("OK",
                notificationService.getAll(ud.getUsername())));
    }

    /** GET /api/notifications/unread-count — badge count only */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(
            @AuthenticationPrincipal UserDetails ud) {
        long count = notificationService.getUnreadCount(ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("OK", Map.of("count", count)));
    }

    /** PATCH /api/notifications/{id}/read */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable String id) {
        notificationService.markRead(ud.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Marked read", null));
    }

    /** PATCH /api/notifications/read-all */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal UserDetails ud) {
        notificationService.markAllRead(ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("All marked read", null));
    }
}
