package com.kidslearn.api.service.impl;

import com.kidslearn.api.entity.Question;
import com.kidslearn.api.entity.QuestionOption;
import com.kidslearn.common.util.RichContentUtil;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

final class QuestionAnswerEvaluator {

    private static final int TYPE_SINGLE = 1;
    private static final int TYPE_ORDER = 2;
    private static final int TYPE_MATCH = 3;
    private static final int TYPE_VOICE = 4;

    private QuestionAnswerEvaluator() {}

    static Evaluation evaluate(Question question, List<QuestionOption> options, String answer) {
        int questionType = question.getQuestionType() == null ? TYPE_SINGLE : question.getQuestionType();
        return switch (questionType) {
            case TYPE_ORDER -> evaluateOrder(options, answer);
            case TYPE_MATCH -> evaluateMatch(options, answer);
            case TYPE_VOICE -> evaluateVoice(options, answer);
            default -> evaluateSingle(options, answer);
        };
    }

    private static Evaluation evaluateSingle(List<QuestionOption> options, String answer) {
        String correctAnswer = options.stream()
            .filter(option -> option.getIsCorrect() != null && option.getIsCorrect() == 1)
            .findFirst()
            .map(QuestionOption::getOptionLabel)
            .orElse("");
        return new Evaluation(correctAnswer.equalsIgnoreCase(safe(answer)), correctAnswer);
    }

    private static Evaluation evaluateOrder(List<QuestionOption> options, String answer) {
        String correctAnswer = sortedOptions(options).stream()
            .map(QuestionOption::getOptionLabel)
            .collect(Collectors.joining(","));
        return new Evaluation(normalizeSequence(correctAnswer).equals(normalizeSequence(answer)), correctAnswer);
    }

    private static Evaluation evaluateMatch(List<QuestionOption> options, String answer) {
        String correctAnswer = sortedOptions(options).stream()
            .map(option -> option.getOptionLabel() + "=" + option.getOptionLabel())
            .collect(Collectors.joining("|"));
        return new Evaluation(normalizePairs(correctAnswer).equals(normalizePairs(answer)), correctAnswer);
    }

    private static Evaluation evaluateVoice(List<QuestionOption> options, String answer) {
        String correctAnswer = options.stream()
            .filter(option -> option.getIsCorrect() != null && option.getIsCorrect() == 1)
            .findFirst()
            .map(QuestionOption::getOptionContent)
            .map(RichContentUtil::toPlainText)
            .orElse("");
        String normalizedCorrect = normalizeSpeech(correctAnswer);
        String normalizedAnswer = normalizeSpeech(answer);
        boolean correct = !normalizedCorrect.isBlank()
            && (normalizedAnswer.equals(normalizedCorrect) || normalizedAnswer.contains(normalizedCorrect));
        return new Evaluation(correct, correctAnswer);
    }

    private static List<QuestionOption> sortedOptions(List<QuestionOption> options) {
        return options.stream()
            .sorted(Comparator
                .comparing((QuestionOption option) -> option.getSortOrder() == null ? Integer.MAX_VALUE : option.getSortOrder())
                .thenComparing(option -> safe(option.getOptionLabel())))
            .toList();
    }

    private static String normalizeSequence(String value) {
        return safe(value).replaceAll("\\s+", "").toUpperCase();
    }

    private static String normalizePairs(String value) {
        return safe(value).replaceAll("\\s+", "").toUpperCase().lines()
            .findFirst()
            .orElse("")
            .transform(line -> {
                if (line.isBlank()) {
                    return "";
                }
                return java.util.Arrays.stream(line.split("\\|"))
                    .filter(pair -> !pair.isBlank())
                    .sorted()
                    .collect(Collectors.joining("|"));
            });
    }

    private static String normalizeSpeech(String value) {
        return safe(value)
            .trim()
            .toLowerCase()
            .replaceAll("[\\s\\p{Punct}，。！？；：、“”‘’]", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    record Evaluation(boolean correct, String correctAnswer) {}
}
