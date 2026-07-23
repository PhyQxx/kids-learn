package com.kidslearn.api.service.impl;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ChallengeSeasonCatalogTest {
    @Test void seasonIsStableForFourWeeksAndRollsOnMonday() {
        var first = ChallengeSeasonCatalog.current(LocalDate.of(2026, 1, 5));
        assertEquals(first.key(), ChallengeSeasonCatalog.current(LocalDate.of(2026, 2, 1)).key());
        assertNotEquals(first.key(), ChallengeSeasonCatalog.current(LocalDate.of(2026, 2, 2)).key());
        assertEquals(LocalDate.of(2026, 2, 1), first.end());
    }
}
