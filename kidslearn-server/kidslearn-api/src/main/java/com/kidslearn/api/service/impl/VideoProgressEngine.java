package com.kidslearn.api.service.impl;

final class VideoProgressEngine {

    private static final int COMPLETE_THRESHOLD_PERCENT = 90;

    private VideoProgressEngine() {
    }

    static Progress evaluate(Integer progressSeconds, Integer durationSeconds) {
        int duration = nonNegative(durationSeconds);
        int progress = nonNegative(progressSeconds);
        int clampedProgress = duration > 0 ? Math.min(progress, duration) : progress;
        int percent = duration > 0 ? Math.min(100, Math.round(clampedProgress * 100f / duration)) : 0;
        return new Progress(clampedProgress, duration, percent, duration > 0 && percent >= COMPLETE_THRESHOLD_PERCENT);
    }

    private static int nonNegative(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    record Progress(int progressSeconds, int durationSeconds, int progressPercent, boolean completed) {
    }
}
