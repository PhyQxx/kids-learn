package com.kidslearn.api.service.impl;

import com.kidslearn.common.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;

public final class SubscriptionPlanCatalog {

    private static final List<SubscriptionPlan> PLANS = List.of(
        new SubscriptionPlan(1, "monthly", "月卡", new BigDecimal("25.00"), new BigDecimal("30.00"), 30, false, false),
        new SubscriptionPlan(2, "annual", "年卡", new BigDecimal("168.00"), new BigDecimal("300.00"), 365, false, true),
        new SubscriptionPlan(3, "permanent", "永久", new BigDecimal("298.00"), new BigDecimal("398.00"), 0, true, false)
    );

    private SubscriptionPlanCatalog() {}

    public static List<SubscriptionPlan> listPlans() {
        return PLANS;
    }

    public static SubscriptionPlan requirePlan(Integer planType) {
        return PLANS.stream()
            .filter(plan -> plan.planType().equals(planType))
            .findFirst()
            .orElseThrow(() -> new BusinessException("会员套餐不存在"));
    }
}

