package com.kidslearn.api.controller;

import com.kidslearn.api.dto.notification.NotificationVO;
import com.kidslearn.api.service.NotificationService;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "通知接口")
@RestController
@RequestMapping("/api/v1/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "获取通知列表")
    @GetMapping("/list")
    public R<List<NotificationVO>> getNotifications(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(notificationService.getNotifications(userId, page, pageSize));
    }

    @Operation(summary = "获取未读数量")
    @GetMapping("/unread-count")
    public R<Map<String, Object>> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        long count = notificationService.getUnreadCount(userId);
        return R.ok(Map.of("count", count));
    }

    @Operation(summary = "标记已读")
    @PostMapping("/read")
    public R<Void> markAsRead(HttpServletRequest request, @RequestParam Long notificationId) {
        Long userId = (Long) request.getAttribute("userId");
        notificationService.markAsRead(userId, notificationId);
        return R.ok();
    }

    @Operation(summary = "全部已读")
    @PostMapping("/read-all")
    public R<Void> markAllAsRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        notificationService.markAllAsRead(userId);
        return R.ok();
    }
}
