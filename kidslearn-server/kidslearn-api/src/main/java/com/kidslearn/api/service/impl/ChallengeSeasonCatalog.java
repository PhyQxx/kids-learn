package com.kidslearn.api.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

final class ChallengeSeasonCatalog {
    private static final LocalDate EPOCH = LocalDate.of(2026, 1, 5); // Monday

    private ChallengeSeasonCatalog() {}

    static Season current(LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long index = Math.floorDiv(ChronoUnit.WEEKS.between(EPOCH, monday), 4);
        LocalDate start = EPOCH.plusWeeks(index * 4);
        LocalDate end = start.plusDays(27);
        return new Season("S" + start.toString().replace("-", ""), start, end, start + " 至 " + end);
    }

    record Season(String key, LocalDate start, LocalDate end, String name) {}
}
