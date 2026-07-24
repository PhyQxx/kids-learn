package com.kidslearn.api.service.impl;

import com.kidslearn.api.entity.Subscription;
import java.time.LocalDateTime;

public final class SubscriptionActivationEngine {

    public static final int STATUS_ACTIVE = 1;
    /** 订阅已过期（由定时任务翻新，不依赖此状态做权益判定，权益判定看 endTime 兜底） */
    public static final int STATUS_EXPIRED = 2;
    public static final LocalDateTime PERMANENT_END_TIME = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private SubscriptionActivationEngine() {}

    public static Subscription activate(
            Subscription existing,
            Long userId,
            SubscriptionPlan plan,
            LocalDateTime now) {
        Subscription subscription = existing != null ? existing : new Subscription();
        LocalDateTime activeEnd = existing != null && existing.getEndTime() != null
            && existing.getEndTime().isAfter(now)
            ? existing.getEndTime()
            : now;
        LocalDateTime startTime = existing != null && existing.getStartTime() != null
            && existing.getEndTime() != null && existing.getEndTime().isAfter(now)
            ? existing.getStartTime()
            : now;

        subscription.setUserId(userId);
        subscription.setPlanType(plan.planType());
        subscription.setStatus(STATUS_ACTIVE);
        subscription.setStartTime(startTime);
        subscription.setEndTime(plan.permanent() ? PERMANENT_END_TIME : activeEnd.plusDays(plan.durationDays()));
        subscription.setAutoRenew(0);
        return subscription;
    }
}

