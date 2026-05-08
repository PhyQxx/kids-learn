package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SubscriptionPlanCatalogTest {

    @Test
    void exposesMonthlyAnnualAndPermanentPlans() {
        assertEquals(3, SubscriptionPlanCatalog.listPlans().size());

        SubscriptionPlan monthly = SubscriptionPlanCatalog.requirePlan(1);
        assertEquals("monthly", monthly.planCode());
        assertEquals(new BigDecimal("25.00"), monthly.amount());
        assertEquals(30, monthly.durationDays());

        SubscriptionPlan annual = SubscriptionPlanCatalog.requirePlan(2);
        assertEquals("annual", annual.planCode());
        assertEquals(new BigDecimal("168.00"), annual.amount());
        assertEquals(365, annual.durationDays());
        assertTrue(annual.recommended());

        SubscriptionPlan permanent = SubscriptionPlanCatalog.requirePlan(3);
        assertEquals("permanent", permanent.planCode());
        assertTrue(permanent.permanent());
    }
}

