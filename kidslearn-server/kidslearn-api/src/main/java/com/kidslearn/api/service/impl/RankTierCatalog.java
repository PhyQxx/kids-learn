package com.kidslearn.api.service.impl;

import java.util.List;

public final class RankTierCatalog {

    private static final List<Tier> TIERS = List.of(
        new Tier("青铜", 0),
        new Tier("白银", 200),
        new Tier("黄金", 500),
        new Tier("铂金", 900),
        new Tier("钻石", 1400)
    );

    private RankTierCatalog() {}

    public static TierProgress resolve(long points) {
        long safePoints = Math.max(0, points);
        Tier current = TIERS.get(0);
        Tier next = null;
        for (int i = 0; i < TIERS.size(); i++) {
            Tier tier = TIERS.get(i);
            if (safePoints >= tier.minPoints()) {
                current = tier;
                next = i + 1 < TIERS.size() ? TIERS.get(i + 1) : null;
            }
        }

        if (next == null) {
            return new TierProgress(current.name(), "", safePoints, 0, 100);
        }

        long span = next.minPoints() - current.minPoints();
        long gained = safePoints - current.minPoints();
        int progressPercent = span > 0 ? (int) Math.min(100, gained * 100 / span) : 100;
        return new TierProgress(
            current.name(),
            next.name(),
            safePoints,
            next.minPoints() - safePoints,
            progressPercent
        );
    }

    private record Tier(String name, long minPoints) {}

    public record TierProgress(
        String tierName,
        String nextTierName,
        long points,
        long pointsToNext,
        int progressPercent
    ) {}
}
