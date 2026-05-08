package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VideoProgressEngineTest {

    @Test
    void clampsProgressAndCalculatesPercent() {
        VideoProgressEngine.Progress progress = VideoProgressEngine.evaluate(130, 100);

        assertEquals(100, progress.progressSeconds());
        assertEquals(100, progress.progressPercent());
        assertTrue(progress.completed());
    }

    @Test
    void marksVideoCompleteAtNinetyPercent() {
        VideoProgressEngine.Progress progress = VideoProgressEngine.evaluate(540, 600);

        assertEquals(90, progress.progressPercent());
        assertTrue(progress.completed());
    }

    @Test
    void keepsZeroDurationIncomplete() {
        VideoProgressEngine.Progress progress = VideoProgressEngine.evaluate(10, 0);

        assertEquals(10, progress.progressSeconds());
        assertEquals(0, progress.progressPercent());
        assertFalse(progress.completed());
    }
}
