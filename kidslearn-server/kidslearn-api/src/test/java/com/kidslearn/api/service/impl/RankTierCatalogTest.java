package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RankTierCatalogTest {

    @Test
    void resolvesBronzeTierProgress() {
        RankTierCatalog.TierProgress progress = RankTierCatalog.resolve(120);

        assertEquals("青铜", progress.tierName());
        assertEquals("白银", progress.nextTierName());
        assertEquals(120, progress.points());
        assertEquals(80, progress.pointsToNext());
        assertEquals(60, progress.progressPercent());
    }

    @Test
    void resolvesTopTierAsComplete() {
        RankTierCatalog.TierProgress progress = RankTierCatalog.resolve(1500);

        assertEquals("钻石", progress.tierName());
        assertEquals("", progress.nextTierName());
        assertEquals(0, progress.pointsToNext());
        assertEquals(100, progress.progressPercent());
    }
}
