package com.kidslearn.api.service;

import java.util.List;
import java.util.Map;

public interface SubscriptionService {
    List<Map<String, Object>> getPlans();

    Map<String, Object> getCurrentSubscription(Long userId);

    Map<String, Object> activateSubscription(Long userId, Integer planType);
}

