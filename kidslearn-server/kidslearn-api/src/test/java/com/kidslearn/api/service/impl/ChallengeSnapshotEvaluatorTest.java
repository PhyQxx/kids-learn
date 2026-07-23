package com.kidslearn.api.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChallengeSnapshotEvaluatorTest {
    @Test void evaluatesSnapshotWithoutTrustingClientScore() {
        assertTrue(ChallengeSnapshotEvaluator.correct(1, "B", "b"));
        assertTrue(ChallengeSnapshotEvaluator.correct(4, "A,B,C", " A, B, C "));
        assertTrue(ChallengeSnapshotEvaluator.correct(5, "A=1|B=2", "B=2|A=1"));
        assertFalse(ChallengeSnapshotEvaluator.correct(1, "B", "A"));
    }
}
