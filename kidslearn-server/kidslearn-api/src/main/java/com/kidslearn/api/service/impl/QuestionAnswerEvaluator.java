package com.kidslearn.api.service.impl;

import com.kidslearn.api.entity.Question;
import com.kidslearn.api.entity.QuestionOption;
import com.kidslearn.common.util.RichContentUtil;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

final class QuestionAnswerEvaluator {

    // 题型定义同步：
    // 1=单项选择题
    // 2=判断题
    // 3=填空题
    // 4=排序题
    // 5=连线题
    private static final int TYPE_SINGLE = 1;
    private static final int TYPE_TRUE_FALSE = 2;
    private static final int TYPE_FILL = 3;
    private static final int TYPE_ORDER = 4;
    private static final int TYPE_MATCH = 5;

    private QuestionAnswerEvaluator() {}

    static Evaluation evaluate(Question question, List<QuestionOption> options, String answer) {
        int questionType = question.getQuestionType() == null ? TYPE_SINGLE : question.getQuestionType();
        return switch (questionType) {
            case TYPE_ORDER -> evaluateOrder(options, answer);
            case TYPE_MATCH -> evaluateMatch(options, answer);
            case TYPE_FILL -> evaluateFill(options, answer);
            default -> evaluateSingle(options, answer); // SINGLE 和 TRUE_FALSE 的判断逻辑一致
        };
    }

    private static Evaluation evaluateSingle(List<QuestionOption> options, String answer) {
        String correctAnswer = options.stream()
            .filter(option -> option.getIsCorrect() != null && option.getIsCorrect() == 1)
            .findFirst()
            .map(QuestionOption::getOptionLabel)
            .orElse("");
            
        // For True/False, the optionLabel might be empty, and answer value might match optionContent or optionLabel
        // If optionLabel is empty, we match against optionContent
        if (correctAnswer.isEmpty()) {
            correctAnswer = options.stream()
                .filter(option -> option.getIsCorrect() != null && option.getIsCorrect() == 1)
                .findFirst()
                .map(QuestionOption::getOptionContent)
                .orElse("");
        }
        
        return new Evaluation(correctAnswer.equalsIgnoreCase(safe(answer)), correctAnswer);
    }

    private static Evaluation evaluateFill(List<QuestionOption> options, String answer) {
        // 填空题：如果有多个正确的 optionContent，只要匹配其中一个就算对
        List<String> validAnswers = options.stream()
            .filter(option -> option.getIsCorrect() != null && option.getIsCorrect() == 1)
            .map(QuestionOption::getOptionContent)
            .map(String::trim)
            .collect(Collectors.toList());
            
        String normalizedAnswer = safe(answer).trim();
        boolean correct = false;
        String firstCorrectAnswer = validAnswers.isEmpty() ? "" : validAnswers.get(0);
        
        for (String valid : validAnswers) {
            if (valid.equalsIgnoreCase(normalizedAnswer)) {
                correct = true;
                break;
            }
        }
        
        return new Evaluation(correct, firstCorrectAnswer);
    }

    private static Evaluation evaluateOrder(List<QuestionOption> options, String answer) {
        String correctAnswer = sortedOptions(options).stream()
            .map(QuestionOption::getOptionLabel)
            .map(label -> label.isEmpty() ? "_" : label) // fallback if no label
            .collect(Collectors.joining(","));
            
        // 如果前台提交的是 content 的组合（比如没有 label 时）
        if (correctAnswer.replace("_", "").replace(",", "").isEmpty()) {
             correctAnswer = sortedOptions(options).stream()
                .map(QuestionOption::getOptionContent)
                .map(RichContentUtil::toPlainText)
                .collect(Collectors.joining(","));
        }
        
        return new Evaluation(normalizeSequence(correctAnswer).equals(normalizeSequence(answer)), correctAnswer);
    }

    private static Evaluation evaluateMatch(List<QuestionOption> options, String answer) {
        // 连线题：左侧(Label) 和 右侧(Content) 配对
        String correctAnswer = options.stream()
            .map(option -> option.getOptionLabel() + "=" + option.getOptionLabel()) // Wait, previously it was label=label ?? That was a bug!
            .collect(Collectors.joining("|"));
            
        // Fix the matching logic: optionLabel=optionContent
        String fixedCorrectAnswer = options.stream()
            .map(option -> safe(option.getOptionLabel()) + "=" + safe(option.getOptionContent()))
            .collect(Collectors.joining("|"));
            
        return new Evaluation(normalizePairs(fixedCorrectAnswer).equals(normalizePairs(answer)), fixedCorrectAnswer);
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

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    record Evaluation(boolean correct, String correctAnswer) {}
}
