package com.kidslearn.api.service.impl;

import java.util.Arrays;
import java.util.stream.Collectors;

final class ChallengeSnapshotEvaluator {
    private ChallengeSnapshotEvaluator() {}

    static boolean correct(Integer type, String expected, String actual) {
        int value = type == null ? 1 : type;
        return switch (value) {
            case 4 -> sequence(expected).equals(sequence(actual));
            case 5 -> pairs(expected).equals(pairs(actual));
            case 6 -> speech(expected).equals(speech(actual));
            case 3 -> Arrays.stream(safe(expected).split("\\|\\|"))
                .anyMatch(candidate -> candidate.trim().equalsIgnoreCase(safe(actual).trim()));
            default -> safe(expected).trim().equalsIgnoreCase(safe(actual).trim());
        };
    }

    private static String sequence(String value) { return safe(value).replaceAll("\\s+", "").toUpperCase(); }
    private static String pairs(String value) {
        return Arrays.stream(safe(value).replaceAll("\\s+", "").toUpperCase().split("\\|"))
            .filter(v -> !v.isBlank()).sorted().collect(Collectors.joining("|"));
    }
    private static String speech(String value) {
        return safe(value).toLowerCase().replaceAll("[\\s\\p{Punct}\\p{IsPunctuation}]+", "");
    }
    private static String safe(String value) { return value == null ? "" : value; }
}
