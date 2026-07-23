package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kidslearn.api.dto.notification.NotificationVO;
import com.kidslearn.api.entity.Notification;
import com.kidslearn.api.mapper.NotificationMapper;
import com.kidslearn.api.entity.UserNotificationPreference;
import com.kidslearn.api.mapper.UserNotificationPreferenceMapper;
import com.kidslearn.api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import com.kidslearn.common.exception.BusinessException;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserNotificationPreferenceMapper preferenceMapper;
    private final ParentPinService parentPinService;
    private static final Set<String> TYPES = Set.of("ACHIEVEMENT_UNLOCKED", "FRIEND_REQUEST", "FRIEND_RESULT",
        "CHALLENGE_INVITE", "CHALLENGE_RESULT", "SUBSCRIPTION_EXPIRING", "LEARNING_REMINDER",
        "TIME_CONTROL_WARNING", "SYSTEM_ANNOUNCEMENT", "RANKING_CHANGE", "ACCOUNT_SECURITY");

    @Override
    public List<NotificationVO> getNotifications(Long userId, Integer page, Integer pageSize) {
        List<Notification> notifications = notificationMapper.selectList(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .and(w -> w.isNull(Notification::getExpireTime).or().gt(Notification::getExpireTime, LocalDateTime.now()))
                .orderByDesc(Notification::getCreateTime)
                .last("LIMIT " + pageSize + " OFFSET " + (page - 1) * pageSize)
        );

        List<NotificationVO> result = new ArrayList<>();
        for (Notification n : notifications) {
            NotificationVO vo = new NotificationVO();
            vo.setId(n.getId());
            vo.setType(n.getType());
            vo.setTitle(n.getTitle());
            vo.setContent(n.getContent());
            vo.setActionType(n.getActionType());
            vo.setActionTarget(n.getActionTarget());
            vo.setExpireTime(n.getExpireTime());
            vo.setIsRead(n.getIsRead() == 1);
            vo.setCreateTime(n.getCreateTime());
            result.add(vo);
        }

        return result;
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationMapper.selectCount(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .and(w -> w.isNull(Notification::getExpireTime).or().gt(Notification::getExpireTime, LocalDateTime.now()))
        );
    }

    @Override
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification != null && notification.getUserId().equals(userId)) {
            notification.setIsRead(1);
            notification.setReadTime(LocalDateTime.now());
            notificationMapper.updateById(notification);
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationMapper.update(null,
            new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1)
                .set(Notification::getReadTime, LocalDateTime.now())
        );
    }

    @Override
    public Map<String, Map<String, Boolean>> getPreferences(Long userId) {
        Map<String, Map<String, Boolean>> result = new LinkedHashMap<>();
        Map<String, UserNotificationPreference> saved = new LinkedHashMap<>();
        preferenceMapper.selectList(new LambdaQueryWrapper<UserNotificationPreference>().eq(UserNotificationPreference::getUserId, userId))
            .forEach(p -> saved.put(p.getEventType(), p));
        for (String type : TYPES) {
            UserNotificationPreference p = saved.get(type);
            boolean defaultInApp = !Set.of("RANKING_CHANGE", "SUBSCRIPTION_EXPIRING").contains(type);
            result.put(type, Map.of("inAppEnabled", p == null ? defaultInApp : !Integer.valueOf(0).equals(p.getInAppEnabled()),
                "pushEnabled", p != null && Integer.valueOf(1).equals(p.getPushEnabled())));
        }
        return result;
    }

    @Override
    public void updatePreference(Long userId, String rawType, Boolean inAppEnabled, Boolean pushEnabled, String parentPin) {
        String type = rawType == null ? "" : rawType.trim().toUpperCase();
        if (!TYPES.contains(type)) throw new BusinessException("不支持的通知类型");
        if (("ACCOUNT_SECURITY".equals(type) || "TIME_CONTROL_WARNING".equals(type)) && Boolean.FALSE.equals(inAppEnabled)) {
            throw new BusinessException("该安全通知不能关闭");
        }
        if (pushEnabled != null) parentPinService.verify(userId, parentPin);
        UserNotificationPreference p = preferenceMapper.selectOne(new LambdaQueryWrapper<UserNotificationPreference>()
            .eq(UserNotificationPreference::getUserId, userId).eq(UserNotificationPreference::getEventType, type).last("LIMIT 1"));
        if (p == null) { p = new UserNotificationPreference(); p.setUserId(userId); p.setEventType(type);
            p.setInAppEnabled(Boolean.FALSE.equals(inAppEnabled) ? 0 : 1); p.setPushEnabled(Boolean.TRUE.equals(pushEnabled) ? 1 : 0); preferenceMapper.insert(p); }
        else { if (inAppEnabled != null) p.setInAppEnabled(inAppEnabled ? 1 : 0); if (pushEnabled != null) p.setPushEnabled(pushEnabled ? 1 : 0); preferenceMapper.updateById(p); }
    }
}
