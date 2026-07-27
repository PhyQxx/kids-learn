package com.kidslearn.api.controller.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kidslearn.api.entity.ContentAudit;
import com.kidslearn.api.entity.Question;
import com.kidslearn.api.entity.QuestionOption;
import com.kidslearn.api.mapper.CourseLevelMapper;
import com.kidslearn.api.mapper.ContentAuditMapper;
import com.kidslearn.api.mapper.QuestionMapper;
import com.kidslearn.api.mapper.QuestionOptionMapper;
import com.kidslearn.api.mapper.SubjectMapper;
import com.kidslearn.api.service.AiService;
import com.kidslearn.api.service.impl.AdminOperationLogService;
import com.kidslearn.common.result.R;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AdminContentAuditControllerTest {

    @Test
    void approvesPendingAuditAndWritesOperationLog() {
        ContentAuditMapper mapper = mock(ContentAuditMapper.class);
        AdminOperationLogService logService = mock(AdminOperationLogService.class);
        AdminContentAuditController controller = controller(mapper, logService);
        ContentAudit audit = new ContentAudit();
        audit.setId(9L);
        audit.setTargetType("QUESTION");
        audit.setTargetId(12L);
        audit.setStatus(AdminContentAuditController.STATUS_PENDING);
        when(mapper.selectById(9L)).thenReturn(audit);

        R<Void> result = controller.review(9L, AdminContentAuditController.STATUS_APPROVED, "ok");

        assertEquals(200, result.getCode());
        assertEquals(AdminContentAuditController.STATUS_APPROVED, audit.getStatus());
        assertEquals("ok", audit.getReviewComment());
        verify(mapper).updateById(audit);
        verify(logService).write("content-audit", "review", "QUESTION", 12L, "audit 9 status 2");
    }

    @Test
    void rejectsInvalidReviewStatus() {
        ContentAuditMapper mapper = mock(ContentAuditMapper.class);
        AdminContentAuditController controller = controller(mapper, mock(AdminOperationLogService.class));

        R<Void> result = controller.review(9L, 4, "bad");

        assertEquals(500, result.getCode());
    }

    @Test
    void undoReviewWithinFiveSecondsReturnsAuditToPending() {
        ContentAuditMapper mapper = mock(ContentAuditMapper.class);
        AdminOperationLogService logService = mock(AdminOperationLogService.class);
        AdminContentAuditController controller = controller(mapper, logService);
        ContentAudit audit = new ContentAudit();
        audit.setId(9L);
        audit.setTargetType("QUESTION");
        audit.setTargetId(12L);
        audit.setStatus(AdminContentAuditController.STATUS_APPROVED);
        audit.setReviewComment("ok");
        audit.setReviewTime(LocalDateTime.now());
        when(mapper.selectById(9L)).thenReturn(audit);

        R<Void> result = controller.undoReview(9L);

        assertEquals(200, result.getCode());
        assertEquals(AdminContentAuditController.STATUS_PENDING, audit.getStatus());
        assertEquals(null, audit.getReviewTime());
        verify(mapper).updateById(audit);
        verify(logService).write("content-audit", "undo-review", "QUESTION", 12L, "undo audit 9");
    }

    @Test
    void undoReviewRejectsExpiredWindow() {
        ContentAuditMapper mapper = mock(ContentAuditMapper.class);
        AdminContentAuditController controller = controller(mapper, mock(AdminOperationLogService.class));
        ContentAudit audit = new ContentAudit();
        audit.setReviewTime(LocalDateTime.now().minusSeconds(6));
        when(mapper.selectById(9L)).thenReturn(audit);

        R<Void> result = controller.undoReview(9L);

        assertEquals(500, result.getCode());
    }

    @Test
    void aiPrecheckQuestionUsesTargetContent() {
        ContentAuditMapper mapper = mock(ContentAuditMapper.class);
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        QuestionOptionMapper optionMapper = mock(QuestionOptionMapper.class);
        AiService aiService = mock(AiService.class);
        AdminContentAuditController controller = new AdminContentAuditController(
            mapper,
            mock(AdminOperationLogService.class),
            mock(SubjectMapper.class),
            mock(CourseLevelMapper.class),
            questionMapper,
            optionMapper,
            aiService
        );
        ContentAudit audit = new ContentAudit();
        audit.setId(9L);
        audit.setTargetType("QUESTION");
        audit.setTargetId(12L);
        when(mapper.selectById(9L)).thenReturn(audit);
        Question question = new Question();
        question.setQuestionContent("3 + 2 = ?");
        question.setAnalysis("答案是5");
        when(questionMapper.selectById(12L)).thenReturn(question);
        QuestionOption option = new QuestionOption();
        option.setOptionLabel("A");
        option.setOptionContent("5");
        option.setIsCorrect(1);
        when(optionMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(option));
        when(aiService.precheckContent("QUESTION", "题目：3 + 2 = ?\n解析：答案是5\n选项：A. 5（正确）"))
            .thenReturn(Map.of("riskLevel", "LOW", "summary", "内容安全，答案唯一"));

        R<Map<String, Object>> result = controller.aiPrecheck(9L);

        assertEquals(200, result.getCode());
        assertEquals("LOW", result.getData().get("riskLevel"));
        assertEquals("内容安全，答案唯一", result.getData().get("summary"));
        verify(aiService).precheckContent("QUESTION", "题目：3 + 2 = ?\n解析：答案是5\n选项：A. 5（正确）");
    }

    @Test
    void aiPrecheckReturnsFailureWhenAuditMissing() {
        ContentAuditMapper mapper = mock(ContentAuditMapper.class);
        AdminContentAuditController controller = controller(mapper, mock(AdminOperationLogService.class));
        when(mapper.selectById(99L)).thenReturn(null);

        R<Map<String, Object>> result = controller.aiPrecheck(99L);

        assertEquals(500, result.getCode());
    }

    private static AdminContentAuditController controller(
        ContentAuditMapper mapper,
        AdminOperationLogService logService
    ) {
        return new AdminContentAuditController(
            mapper,
            logService,
            mock(SubjectMapper.class),
            mock(CourseLevelMapper.class),
            mock(QuestionMapper.class),
            mock(QuestionOptionMapper.class),
            mock(AiService.class)
        );
    }
}
