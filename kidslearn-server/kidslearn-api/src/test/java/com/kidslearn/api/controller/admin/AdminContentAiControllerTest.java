package com.kidslearn.api.controller.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kidslearn.api.mapper.CourseLevelMapper;
import com.kidslearn.api.mapper.QuestionMapper;
import com.kidslearn.api.mapper.QuestionOptionMapper;
import com.kidslearn.api.mapper.SubjectMapper;
import com.kidslearn.api.service.AiService;
import com.kidslearn.api.service.impl.AdminOperationLogService;
import com.kidslearn.api.service.impl.QuestionAudioService;
import com.kidslearn.common.result.R;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdminContentAiControllerTest {

    @Test
    void generatesQuestionDraftThroughAiService() {
        AiService aiService = mock(AiService.class);
        when(aiService.generateQuestionDraft("数学", "一年级", 1, "10以内加法"))
            .thenReturn(Map.of(
                "questionContent", "3 + 2 = ?",
                "analysis", "把3和2合起来是5。",
                "options", List.of(Map.of("optionLabel", "A", "optionContent", "5", "isCorrect", 1))
            ));
        AdminContentController controller = controller(aiService);

        R<Map<String, Object>> result = controller.questionAiGenerate(Map.of(
            "subjectName", "数学",
            "gradeName", "一年级",
            "questionType", 1,
            "knowledgePoint", "10以内加法"
        ));

        assertEquals(200, result.getCode());
        assertEquals("3 + 2 = ?", result.getData().get("questionContent"));
        assertEquals("把3和2合起来是5。", result.getData().get("analysis"));
        verify(aiService).generateQuestionDraft("数学", "一年级", 1, "10以内加法");
    }

    @Test
    void generatesQuestionAnalysisThroughAiService() {
        AiService aiService = mock(AiService.class);
        when(aiService.generateQuestionAnalysis("3 + 2 = ?", "5", List.of("5", "4"), ""))
            .thenReturn("3个再加2个，一共是5个。");
        AdminContentController controller = controller(aiService);

        R<Map<String, String>> result = controller.questionAiAnalysis(Map.of(
            "questionContent", "3 + 2 = ?",
            "correctAnswer", "5",
            "options", List.of("5", "4")
        ));

        assertEquals(200, result.getCode());
        assertEquals("3个再加2个，一共是5个。", result.getData().get("analysis"));
        verify(aiService).generateQuestionAnalysis("3 + 2 = ?", "5", List.of("5", "4"), "");
    }

    @Test
    void returnsFailureWhenAiQuestionDraftIsUnavailable() {
        AiService aiService = mock(AiService.class);
        when(aiService.generateQuestionDraft(any(), any(), any(), any())).thenReturn(Map.of());
        AdminContentController controller = controller(aiService);

        R<Map<String, Object>> result = controller.questionAiGenerate(Map.of("subjectName", "数学"));

        assertEquals(500, result.getCode());
    }

    private static AdminContentController controller(AiService aiService) {
        return new AdminContentController(
            mock(SubjectMapper.class),
            mock(CourseLevelMapper.class),
            mock(QuestionMapper.class),
            mock(QuestionOptionMapper.class),
            mock(QuestionAudioService.class),
            mock(AdminOperationLogService.class),
            aiService
        );
    }
}
