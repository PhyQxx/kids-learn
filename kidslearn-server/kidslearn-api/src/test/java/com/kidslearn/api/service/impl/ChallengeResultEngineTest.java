package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChallengeResultEngineTest {

    @Test
    void winGivesFullRewardAndPositiveRankDelta() {
        ChallengeResultEngine.Result result = ChallengeResultEngine.settle(85, 70);

        assertEquals(1, result.isWinner());
        assertEquals(20, result.rewardGold());
        assertEquals(30, result.rankDelta());
    }

    @Test
    void drawGivesSmallRewardAndRankDelta() {
        ChallengeResultEngine.Result result = ChallengeResultEngine.settle(80, 80);

        assertEquals(2, result.isWinner());
        assertEquals(8, result.rewardGold());
        assertEquals(10, result.rankDelta());
    }

    @Test
    void lossKeepsParticipationRewardAndNegativeRankDelta() {
        ChallengeResultEngine.Result result = ChallengeResultEngine.settle(55, 90);

        assertEquals(0, result.isWinner());
        assertEquals(5, result.rewardGold());
        assertEquals(-12, result.rankDelta());
    }
}
