package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.kidslearn.api.entity.Subscription;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SubscriptionActivationEngineTest {

    @Test
    void extendsActiveSubscriptionFromCurrentEndTime() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 6, 18, 0);
        Subscription existing = new Subscription();
        existing.setUserId(7L);
        existing.setPlanType(1);
        existing.setStatus(1);
        existing.setStartTime(now.minusDays(10));
        existing.setEndTime(now.plusDays(5));

        Subscription activated = SubscriptionActivationEngine.activate(existing, 7L, SubscriptionPlanCatalog.requirePlan(1), now);

        assertSame(existing, activated);
        assertEquals(1, activated.getStatus());
        assertEquals(1, activated.getPlanType());
        assertEquals(now.minusDays(10), activated.getStartTime());
        assertEquals(now.plusDays(35), activated.getEndTime());
    }

    @Test
    void startsNewSubscriptionAtPaymentTimeWhenExistingExpired() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 6, 18, 0);
        Subscription existing = new Subscription();
        existing.setUserId(7L);
        existing.setStatus(1);
        existing.setStartTime(now.minusDays(60));
        existing.setEndTime(now.minusDays(1));

        Subscription activated = SubscriptionActivationEngine.activate(existing, 7L, SubscriptionPlanCatalog.requirePlan(1), now);

        assertEquals(now, activated.getStartTime());
        assertEquals(now.plusDays(30), activated.getEndTime());
    }

    @Test
    void permanentPlanUsesFarFutureEndTime() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 6, 18, 0);

        Subscription activated = SubscriptionActivationEngine.activate(null, 7L, SubscriptionPlanCatalog.requirePlan(3), now);

        assertEquals(3, activated.getPlanType());
        assertEquals(LocalDateTime.of(9999, 12, 31, 23, 59, 59), activated.getEndTime());
    }
}

