package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class AchievementRuleEngineTest {

    @Test
    void resolvesCurrentValueFromConditionType() {
        AchievementRuleEngine.ProgressSnapshot snapshot = new AchievementRuleEngine.ProgressSnapshot(
            8, 3, 1, 1, 2, 2, 5, 7, 4
        );

        assertEquals(3, AchievementRuleEngine.resolveCurrent(
            "{\"type\":\"THREE_STAR\",\"target\":10}", "anything", "anything", snapshot
        ));
        assertEquals(7, AchievementRuleEngine.resolveCurrent(
            "{\"type\":\"STREAK_DAYS\",\"target\":7}", "anything", "anything", snapshot
        ));
    }

    @Test
    void treatsStickerConditionTypeAsCollectedStickerCount() {
        AchievementRuleEngine.ProgressSnapshot snapshot = new AchievementRuleEngine.ProgressSnapshot(
            8, 3, 1, 1, 2, 2, 5, 7, 4
        );

        assertEquals(5, AchievementRuleEngine.resolveCurrent(
            "{\"type\":\"sticker\",\"target\":10}", "anything", "anything", snapshot
        ));
    }

    @Test
    void resolvesPerfectAndMathPerfectAchievements() {
        AchievementRuleEngine.ProgressSnapshot snapshot = new AchievementRuleEngine.ProgressSnapshot(
            8, 3, 2, 1, 1, 2, 5, 7, 4
        );

        assertEquals(2, AchievementRuleEngine.resolveCurrent(
            "{\"type\":\"PERFECT_SCORE\",\"target\":1}", "anything", "anything", snapshot
        ));
        assertEquals(1, AchievementRuleEngine.resolveCurrent(
            "{\"type\":\"MATH_PERFECT\",\"target\":1}", "anything", "anything", snapshot
        ));
        assertEquals(2, AchievementRuleEngine.resolveCurrent(
            null, "anything", "满分达成", snapshot
        ));
        assertEquals(1, AchievementRuleEngine.resolveCurrent(
            null, "anything", "数学满分", snapshot
        ));
    }

    @Test
    void resolvesRewardGoldFromRewardJson() {
        assertEquals(120, AchievementRuleEngine.resolveGoldReward(
            "{\"rewards\":[{\"type\":\"gold\",\"value\":120},{\"type\":\"exp\",\"value\":30}]}", 50
        ));
        assertEquals(80, AchievementRuleEngine.resolveGoldReward(
            "{\"type\":\"GOLD\",\"quantity\":80}", 50
        ));
    }

    @Test
    void resolvesAllRewardItemsFromRewardJson() {
        List<AchievementRuleEngine.RewardItem> rewards = AchievementRuleEngine.resolveRewardItems(
            "{\"rewards\":["
                + "{\"type\":\"gold\",\"value\":120},"
                + "{\"type\":\"exp\",\"value\":30},"
                + "{\"type\":\"sticker\",\"id\":9,\"quantity\":2},"
                + "{\"type\":\"title\",\"titleId\":4}"
                + "]}"
        );

        assertEquals(4, rewards.size());
        assertEquals("GOLD", rewards.get(0).type());
        assertEquals(120, rewards.get(0).quantity());
        assertEquals("STICKER", rewards.get(2).type());
        assertEquals(9L, rewards.get(2).itemId());
        assertEquals("TITLE", rewards.get(3).type());
        assertEquals(4L, rewards.get(3).itemId());
    }
}
