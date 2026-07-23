package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class LearningAccessPolicyTest {

    @Test
    void blocksWhenDailyLimitHasBeenReached() {
        var decision = LearningAccessPolicy.evaluate(
            true,
            true,
            30,
            true,
            LocalTime.of(8, 0),
            LocalTime.of(21, 0),
            30,
            LocalDateTime.of(2026, 7, 22, 10, 0)
        );

        assertFalse(decision.allowed());
        assertEquals(LearningAccessPolicy.DAILY_LIMIT_REACHED, decision.reasonCode());
        assertEquals(LocalDateTime.of(2026, 7, 23, 0, 0), decision.nextAllowedAt());
    }

    @Test
    void treatsConfiguredTimesAsAllowedWindow() {
        var inside = LearningAccessPolicy.evaluate(
            true, true, 60, true, LocalTime.of(8, 0), LocalTime.of(21, 0), 10,
            LocalDateTime.of(2026, 7, 22, 10, 0)
        );
        var outside = LearningAccessPolicy.evaluate(
            true, true, 60, true, LocalTime.of(8, 0), LocalTime.of(21, 0), 10,
            LocalDateTime.of(2026, 7, 22, 22, 0)
        );

        assertTrue(inside.allowed());
        assertFalse(outside.allowed());
        assertEquals(LearningAccessPolicy.OUTSIDE_ALLOWED_TIME, outside.reasonCode());
        assertEquals(LocalDateTime.of(2026, 7, 23, 8, 0), outside.nextAllowedAt());
    }

    @Test
    void supportsOvernightAllowedWindow() {
        var night = LearningAccessPolicy.evaluate(
            true, true, 60, true, LocalTime.of(21, 0), LocalTime.of(7, 0), 10,
            LocalDateTime.of(2026, 7, 22, 22, 0)
        );
        var midday = LearningAccessPolicy.evaluate(
            true, true, 60, true, LocalTime.of(21, 0), LocalTime.of(7, 0), 10,
            LocalDateTime.of(2026, 7, 22, 12, 0)
        );

        assertTrue(night.allowed());
        assertFalse(midday.allowed());
        assertEquals(LocalDateTime.of(2026, 7, 22, 21, 0), midday.nextAllowedAt());
    }

    @Test
    void disabledControlAllowsLearning() {
        var decision = LearningAccessPolicy.evaluate(
            false, true, 1, true, LocalTime.of(8, 0), LocalTime.of(9, 0), 100,
            LocalDateTime.of(2026, 7, 22, 23, 0)
        );

        assertTrue(decision.allowed());
    }

    @Test
    void subRulesCanBeDisabledIndependently() {
        var decision = LearningAccessPolicy.evaluate(
            true, false, 1, false, LocalTime.of(8, 0), LocalTime.of(9, 0), 100,
            LocalDateTime.of(2026, 7, 22, 23, 0)
        );

        assertTrue(decision.allowed());
    }
}
