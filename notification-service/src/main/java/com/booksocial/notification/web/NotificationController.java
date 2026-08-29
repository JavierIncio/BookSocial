package com.booksocial.notification.web;

import com.booksocial.notification.service.NotificationService;
import com.booksocial.notification.web.dto.NotificationResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list(@RequestHeader("X-User-Id") Long userId) {
        return notificationService.listNotifications(userId);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Map.of("count", notificationService.unreadCount(userId));
    }

    @PostMapping("/read")
    public void markAllAsRead(@RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllAsRead(userId);
    }
}
