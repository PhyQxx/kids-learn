package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.*;
import com.kidslearn.api.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Component @RequiredArgsConstructor
public class NotificationOutboxJob {
    private final DomainEventOutboxMapper outboxMapper; private final NotificationMapper notificationMapper;
    private final UserNotificationPreferenceMapper preferenceMapper; private final UserMapper userMapper;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelayString = "${notification.outbox.poll-ms:30000}", initialDelay = 3000)
    public void deliver() {
        List<DomainEventOutbox> events = outboxMapper.selectList(new LambdaQueryWrapper<DomainEventOutbox>()
            .eq(DomainEventOutbox::getStatus, "PENDING").orderByAsc(DomainEventOutbox::getId).last("LIMIT 100"));
        // 空结果直接返回，不开事务，避免每次轮询都打印 SqlSession 开关日志
        if (events.isEmpty()) return;
        for (DomainEventOutbox event : events) deliverOne(event);
    }

    private void deliverOne(DomainEventOutbox event) {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                User user = userMapper.selectById(event.getUserId());
                if (user == null || Integer.valueOf(0).equals(user.getStatus())) { complete(event, "SKIPPED"); return; }
                UserNotificationPreference preference = preferenceMapper.selectOne(new LambdaQueryWrapper<UserNotificationPreference>()
                    .eq(UserNotificationPreference::getUserId, event.getUserId()).eq(UserNotificationPreference::getEventType, event.getEventType()).last("LIMIT 1"));
                if (preference != null && Integer.valueOf(0).equals(preference.getInAppEnabled()) && !mandatory(event.getEventType())) {
                    complete(event, "SKIPPED"); return;
                }
                Notification notification = new Notification(); notification.setUserId(event.getUserId()); notification.setEventId(event.getEventId());
                notification.setType(event.getEventType()); notification.setTitle(event.getTitle()); notification.setContent(event.getContent());
                notification.setActionType(event.getActionType()); notification.setActionTarget(event.getActionTarget());
                notification.setExpireTime(event.getExpireTime()); notification.setIsRead(0); notification.setCreateTime(LocalDateTime.now());
                try { notificationMapper.insert(notification); } catch (DuplicateKeyException ignored) { }
                complete(event, "PROCESSED");
            } catch (Exception e) {
                event.setAttempts((event.getAttempts() == null ? 0 : event.getAttempts()) + 1);
                event.setLastError(e.getClass().getSimpleName());
                if (event.getAttempts() >= 5) event.setStatus("FAILED");
                outboxMapper.updateById(event);
            }
        });
    }
    private void complete(DomainEventOutbox event, String status) {
        event.setStatus(status); event.setProcessedAt(LocalDateTime.now()); outboxMapper.updateById(event);
    }
    private boolean mandatory(String type) { return "ACCOUNT_SECURITY".equals(type) || "TIME_CONTROL_WARNING".equals(type); }
}
