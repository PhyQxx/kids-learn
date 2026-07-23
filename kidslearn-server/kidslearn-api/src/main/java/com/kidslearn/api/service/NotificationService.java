package com.kidslearn.api.service;

import com.kidslearn.api.dto.notification.NotificationVO;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    /** 获取通知列表 */
    List<NotificationVO> getNotifications(Long userId, Integer page, Integer pageSize);

    /** 获取未读数量 */
    long getUnreadCount(Long userId);

    /** 标记已读 */
    void markAsRead(Long userId, Long notificationId);

    /** 全部已读 */
    void markAllAsRead(Long userId);
    Map<String, Map<String, Boolean>> getPreferences(Long userId);
    void updatePreference(Long userId, String type, Boolean inAppEnabled, Boolean pushEnabled, String parentPin);
}
