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

    @Operation(summary = "获取账号通知偏好")
    @GetMapping("/preferences")
    public R<Map<String, Map<String, Boolean>>> preferences(HttpServletRequest request) {
        return R.ok(notificationService.getPreferences((Long) request.getAttribute("userId")));
    }

    @Operation(summary = "保存账号通知偏好")
    @PutMapping("/preferences/{type}")
    public R<Void> preference(HttpServletRequest request, @PathVariable String type, @RequestBody Map<String, Object> body) {
        Boolean inApp = body.get("inAppEnabled") instanceof Boolean b ? b : null;
        Boolean push = body.get("pushEnabled") instanceof Boolean b ? b : null;
        String pin = body.get("parentPin") instanceof String s ? s : null;
        notificationService.updatePreference((Long) request.getAttribute("userId"), type, inApp, push, pin);
        return R.ok();
    }
}
