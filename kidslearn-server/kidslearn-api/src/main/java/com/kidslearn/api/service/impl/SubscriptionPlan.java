package com.kidslearn.api.service.impl;

import java.math.BigDecimal;

public record SubscriptionPlan(
    Integer planType,
    String planCode,
    String planName,
    BigDecimal amount,
    BigDecimal originalAmount,
    Integer durationDays,
    boolean permanent,
    boolean recommended
) {}

