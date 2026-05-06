package com.kidslearn.api.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;

final class PetEvolutionEngine {

    static final int FEED_EXP = 5;
    static final int PLAY_EXP = 3;
    static final int BATH_EXP = 2;

    static final int HUNGER_DECAY_PER_HOUR = 2;
    static final int ENERGY_DECAY_PER_HOUR = 1;
    static final int MOOD_DECAY_THRESHOLD_HUNGER = 20;
    static final int MAX_LEVEL = 30;

    private static final int[] LEVEL_EXP = buildLevelExpTable();

    private PetEvolutionEngine() {}

    private static int[] buildLevelExpTable() {
        int[] table = new int[MAX_LEVEL + 1];
        table[1] = 0;
        for (int i = 2; i <= MAX_LEVEL; i++) {
            table[i] = table[i - 1] + (i - 1) * 50;
        }
        return table;
    }

    static int calculateLevel(int totalExp) {
        for (int i = MAX_LEVEL; i >= 1; i--) {
            if (totalExp >= LEVEL_EXP[i]) {
                return i;
            }
        }
        return 1;
    }

    static int expForNextLevel(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) {
            return LEVEL_EXP[MAX_LEVEL] - LEVEL_EXP[MAX_LEVEL - 1];
        }
        return LEVEL_EXP[currentLevel + 1] - LEVEL_EXP[currentLevel];
    }

    static int expInCurrentLevel(int totalExp, int currentLevel) {
        int base = LEVEL_EXP[currentLevel];
        int next = currentLevel < MAX_LEVEL ? LEVEL_EXP[currentLevel + 1] : base + 50;
        int range = next - base;
        int progress = totalExp - base;
        return Math.min(progress, range);
    }

    static int expNeededForNextLevel(int totalExp, int currentLevel) {
        if (currentLevel >= MAX_LEVEL) return 0;
        int next = LEVEL_EXP[currentLevel + 1];
        return Math.max(0, next - totalExp);
    }

    record DecayResult(int hunger, int mood, int energy, boolean changed) {}

    static DecayResult applyPassiveDecay(int hunger, int mood, int energy,
                                          LocalDateTime lastActiveTime) {
        if (lastActiveTime == null) {
            return new DecayResult(hunger, mood, energy, false);
        }
        long hours = Duration.between(lastActiveTime, LocalDateTime.now()).toHours();
        if (hours <= 0) {
            return new DecayResult(hunger, mood, energy, false);
        }
        int newHunger = Math.max(0, hunger - (int)(hours * HUNGER_DECAY_PER_HOUR));
        int newEnergy = Math.max(0, energy - (int)(hours * ENERGY_DECAY_PER_HOUR));
        int newMood = mood;
        if (newHunger < MOOD_DECAY_THRESHOLD_HUNGER && newMood > 1) {
            newMood = Math.max(1, newMood - 1);
        }
        boolean changed = (newHunger != hunger || newEnergy != energy || newMood != mood);
        return new DecayResult(newHunger, newMood, newEnergy, changed);
    }
}
