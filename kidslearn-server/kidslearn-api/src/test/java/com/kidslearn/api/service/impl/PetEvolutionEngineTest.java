package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PetEvolutionEngineTest {

    @Test
    void reportsProgressWithinCurrentLevel() {
        assertEquals(2, PetEvolutionEngine.calculateLevel(80));
        assertEquals(30, PetEvolutionEngine.expInCurrentLevel(80, 2));
        assertEquals(100, PetEvolutionEngine.expForNextLevel(2));
        assertEquals(70, PetEvolutionEngine.expNeededForNextLevel(80, 2));
    }

    @Test
    void maxLevelHasNoRemainingRequiredExp() {
        int maxLevel = PetEvolutionEngine.MAX_LEVEL;

        assertEquals(0, PetEvolutionEngine.expNeededForNextLevel(999999, maxLevel));
        assertTrue(PetEvolutionEngine.expForNextLevel(maxLevel) > 0);
    }

    @Test
    void passiveDecayLowersNeedsAfterInactiveHours() {
        PetEvolutionEngine.DecayResult result = PetEvolutionEngine.applyPassiveDecay(
            30,
            3,
            20,
            LocalDateTime.now().minusHours(5)
        );

        assertEquals(20, result.hunger());
        assertEquals(15, result.energy());
        assertEquals(3, result.mood());
        assertTrue(result.changed());
    }
}
