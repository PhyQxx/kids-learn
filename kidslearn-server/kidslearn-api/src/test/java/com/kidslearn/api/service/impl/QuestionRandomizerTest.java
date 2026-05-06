package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.kidslearn.api.entity.Question;
import com.kidslearn.api.entity.QuestionOption;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class QuestionRandomizerTest {

    @Test
    void shufflesQuestionOrderWithoutLosingQuestions() {
        List<Question> questions = List.of(question(1L), question(2L), question(3L), question(4L), question(5L));

        List<Question> shuffled = QuestionRandomizer.shuffledCopy(questions, new Random(3));

        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), questions.stream().map(Question::getId).toList());
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), shuffled.stream().map(Question::getId).sorted().toList());
        assertNotEquals(questions.stream().map(Question::getId).toList(), shuffled.stream().map(Question::getId).toList());
    }

    @Test
    void randomizesOptionsAndPreservesOriginalAnswerValue() {
        List<QuestionOption> options = List.of(
            option("A", "one"),
            option("B", "two"),
            option("C", "three"),
            option("D", "four")
        );

        List<Map<String, String>> randomized = QuestionRandomizer.toRandomizedOptions(options, new Random(5));

        assertEquals(List.of("A", "B", "C", "D"), randomized.stream().map(o -> o.get("optionLabel")).toList());
        assertEquals(List.of("A", "B", "C", "D"), options.stream().map(QuestionOption::getOptionLabel).toList());
        assertEquals(List.of("A", "B", "C", "D"), randomized.stream().map(o -> o.get("answerValue")).sorted().toList());
        assertNotEquals(
            options.stream().map(QuestionOption::getOptionLabel).toList(),
            randomized.stream().map(o -> o.get("answerValue")).toList()
        );
    }

    @Test
    void exposesSpeechFieldsForRandomizedOptions() {
        String optionContent = "{\"type\":\"richText\",\"version\":1,"
            + "\"speech\":{\"text\":\"Read option\",\"audioUrl\":\"https://example.com/a.mp3\"},"
            + "\"blocks\":["
            + "{\"type\":\"paragraph\",\"text\":\"Visible option\"},"
            + "{\"type\":\"image\",\"url\":\"https://example.com/a.png\",\"alt\":\"Option image\"}"
            + "]}";
        List<QuestionOption> options = List.of(option("A", optionContent));

        Map<String, String> randomized = QuestionRandomizer.toRandomizedOptions(options, new Random(1)).get(0);

        assertEquals("Visible option Option image", randomized.get("optionText"));
        assertEquals("Read option", randomized.get("optionSpeechText"));
        assertEquals("https://example.com/a.mp3", randomized.get("optionAudioUrl"));
    }

    private Question question(Long id) {
        Question question = new Question();
        question.setId(id);
        return question;
    }

    private QuestionOption option(String label, String content) {
        QuestionOption option = new QuestionOption();
        option.setOptionLabel(label);
        option.setOptionContent(content);
        return option;
    }
}
