package com.doumi.donation.notification.controller;

import com.doumi.donation.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<?> getMyNotifications(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(notificationService.getMyNotifications(memberId));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<?> markRead(@AuthenticationPrincipal Long memberId,
                                      @PathVariable long notificationId) {
        notificationService.markRead(memberId, notificationId);
        return ResponseEntity.ok("알림을 읽음 처리했습니다.");
    }

    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllRead(@AuthenticationPrincipal Long memberId) {
        notificationService.markAllRead(memberId);
        return ResponseEntity.ok("모든 알림을 읽음 처리했습니다.");
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> deleteNotification(@AuthenticationPrincipal Long memberId,
                                                @PathVariable long notificationId) {
        notificationService.deleteNotification(memberId, notificationId);
        return ResponseEntity.ok("알림을 삭제했습니다.");
    }
}
