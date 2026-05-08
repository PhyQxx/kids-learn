package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.Subscription;
import com.kidslearn.api.mapper.SubscriptionMapper;
import com.kidslearn.api.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionMapper subscriptionMapper;

    @Override
    public List<Map<String, Object>> getPlans() {
        return SubscriptionPlanCatalog.listPlans().stream()
            .map(this::toPlanMap)
            .toList();
    }

    @Override
    public Map<String, Object> getCurrentSubscription(Long userId) {
        Subscription subscription = findLatestSubscription(userId);
        if (subscription == null) {
            return Map.of("active", false);
        }
        Map<String, Object> result = toSubscriptionMap(subscription);
        result.put("active", isActive(subscription));
        return result;
    }

    @Override
    public Map<String, Object> activateSubscription(Long userId, Integer planType) {
        SubscriptionPlan plan = SubscriptionPlanCatalog.requirePlan(planType);
        Subscription existing = findLatestSubscription(userId);
        Subscription subscription = SubscriptionActivationEngine.activate(existing, userId, plan, LocalDateTime.now());

        if (subscription.getId() == null) {
            subscriptionMapper.insert(subscription);
        } else {
            subscriptionMapper.updateById(subscription);
        }

        Map<String, Object> result = toSubscriptionMap(subscription);
        result.put("active", true);
        return result;
    }

    private Subscription findLatestSubscription(Long userId) {
        return subscriptionMapper.selectOne(
            new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getUserId, userId)
                .orderByDesc(Subscription::getEndTime)
                .last("LIMIT 1")
        );
    }

    private boolean isActive(Subscription subscription) {
        return subscription.getStatus() != null
            && subscription.getStatus() == SubscriptionActivationEngine.STATUS_ACTIVE
            && subscription.getEndTime() != null
            && !subscription.getEndTime().isBefore(LocalDateTime.now());
    }

    private Map<String, Object> toPlanMap(SubscriptionPlan plan) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("planType", plan.planType());
        map.put("planCode", plan.planCode());
        map.put("planName", plan.planName());
        map.put("amount", plan.amount());
        map.put("originalAmount", plan.originalAmount());
        map.put("durationDays", plan.durationDays());
        map.put("permanent", plan.permanent());
        map.put("recommended", plan.recommended());
        return map;
    }

    private Map<String, Object> toSubscriptionMap(Subscription subscription) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", subscription.getId());
        map.put("userId", subscription.getUserId());
        map.put("planType", subscription.getPlanType());
        map.put("status", subscription.getStatus());
        map.put("startTime", subscription.getStartTime());
        map.put("endTime", subscription.getEndTime());
        map.put("autoRenew", subscription.getAutoRenew());
        return map;
    }
}

