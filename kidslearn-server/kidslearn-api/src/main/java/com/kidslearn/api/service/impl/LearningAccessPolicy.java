package com.kidslearn.api.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Pure time-control policy. Database access stays in {@link LearningAccessService};
 * this class is deliberately side-effect free so boundary behavior can be tested.
 */
final class LearningAccessPolicy {

    static final String ALLOWED = "ALLOWED";
    static final String DAILY_LIMIT_REACHED = "DAILY_LIMIT_REACHED";
    static final String OUTSIDE_ALLOWED_TIME = "OUTSIDE_ALLOWED_TIME";

    private LearningAccessPolicy() {}

    static Decision evaluate(
            boolean enabled,
            boolean limitEnabled,
            Integer dailyLimitMinutes,
            boolean allowedWindowEnabled,
            LocalTime allowedStart,
            LocalTime allowedEnd,
            int usedMinutes,
            LocalDateTime now) {
        if (!enabled) {
            return Decision.allowed(usedMinutes, dailyLimitMinutes);
        }

        int safeUsedMinutes = Math.max(0, usedMinutes);
        if (limitEnabled
                && dailyLimitMinutes != null
                && dailyLimitMinutes > 0
                && safeUsedMinutes >= dailyLimitMinutes) {
            LocalDateTime nextAllowedAt = now.toLocalDate().plusDays(1).atStartOfDay();
            return new Decision(
                false,
                DAILY_LIMIT_REACHED,
                "今日学习已达到 " + dailyLimitMinutes + " 分钟上限，明天再来吧",
                safeUsedMinutes,
                dailyLimitMinutes,
                nextAllowedAt
            );
        }

        if (allowedWindowEnabled
                && allowedStart != null
                && allowedEnd != null
                && !allowedStart.equals(allowedEnd)) {
            LocalTime current = now.toLocalTime();
            if (!isWithinAllowedWindow(current, allowedStart, allowedEnd)) {
                LocalDateTime nextAllowedAt = nextAllowedAt(now, allowedStart);
                return new Decision(
                    false,
                    OUTSIDE_ALLOWED_TIME,
                    "当前不在允许学习时段内，请在 " + allowedStart + "-" + allowedEnd + " 学习",
                    safeUsedMinutes,
                    dailyLimitMinutes,
                    nextAllowedAt
                );
            }
        }

        return Decision.allowed(safeUsedMinutes, dailyLimitMinutes);
    }

    private static boolean isWithinAllowedWindow(LocalTime current, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            return !current.isBefore(start) && !current.isAfter(end);
        }
        return !current.isBefore(start) || !current.isAfter(end);
    }

    private static LocalDateTime nextAllowedAt(LocalDateTime now, LocalTime allowedStart) {
        LocalDate today = now.toLocalDate();
        LocalDateTime todayStart = today.atTime(allowedStart);
        return now.isBefore(todayStart) ? todayStart : today.plusDays(1).atTime(allowedStart);
    }

    record Decision(
        boolean allowed,
        String reasonCode,
        String message,
        int usedMinutes,
        Integer limitMinutes,
        LocalDateTime nextAllowedAt
    ) {
        static Decision allowed(int usedMinutes, Integer limitMinutes) {
            return new Decision(true, ALLOWED, "", Math.max(0, usedMinutes), limitMinutes, null);
        }
    }
}
