package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kidslearn.api.entity.Question;
import com.kidslearn.api.entity.QuestionOption;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionAnswerEvaluatorTest {

    @Test
    void evaluatesOrderAnswerByOriginalOptionOrder() {
        Question question = question(4);
        List<QuestionOption> options = List.of(
            option("A", "我", 0, 1),
            option("B", "爱", 0, 2),
            option("C", "学习", 0, 3)
        );

        assertTrue(QuestionAnswerEvaluator.evaluate(question, options, "A,B,C").correct());
        assertFalse(QuestionAnswerEvaluator.evaluate(question, options, "B,A,C").correct());
    }

    @Test
    void evaluatesMatchAnswerByPairs() {
        Question question = question(5);
        List<QuestionOption> options = List.of(
            option("A", "{\"left\":\"大\",\"right\":\"小\"}", 0, 1),
            option("B", "{\"left\":\"上\",\"right\":\"下\"}", 0, 2)
        );

        assertTrue(QuestionAnswerEvaluator.evaluate(question, options, "B=B|A=A").correct());
        assertFalse(QuestionAnswerEvaluator.evaluate(question, options, "A=B|B=A").correct());
    }

    @Test
    void evaluatesVoiceAnswerWithPunctuationTolerance() {
        Question question = question(6);
        List<QuestionOption> options = List.of(option("A", "I like apples", 1, 1));

        assertTrue(QuestionAnswerEvaluator.evaluate(question, options, "I like apples!").correct());
        assertFalse(QuestionAnswerEvaluator.evaluate(question, options, "I like bananas").correct());
    }

    private Question question(int type) {
        Question question = new Question();
        question.setQuestionType(type);
        return question;
    }

    private QuestionOption option(String label, String content, int isCorrect, int sortOrder) {
        QuestionOption option = new QuestionOption();
        option.setOptionLabel(label);
        option.setOptionContent(content);
        option.setIsCorrect(isCorrect);
        option.setSortOrder(sortOrder);
        return option;
    }
}
