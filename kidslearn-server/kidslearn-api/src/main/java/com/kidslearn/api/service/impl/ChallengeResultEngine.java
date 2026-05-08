package com.kidslearn.api.service.impl;

public final class ChallengeResultEngine {

    private ChallengeResultEngine() {}

    public static Result settle(Integer userScore, Integer opponentScore) {
        int mine = Math.max(0, userScore == null ? 0 : userScore);
        int opponent = Math.max(0, opponentScore == null ? 0 : opponentScore);
        if (mine > opponent) {
            return new Result(1, 20, 30);
        }
        if (mine == opponent) {
            return new Result(2, 8, 10);
        }
        return new Result(0, 5, -12);
    }

    public record Result(int isWinner, int rewardGold, int rankDelta) {}
}
