package com.kidslearn.api.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.kidslearn.api.mapper.*;
import com.kidslearn.api.service.AiService;
import com.kidslearn.api.service.impl.AdminOperationLogService;
import com.kidslearn.api.service.impl.QuestionAudioService;
import org.junit.jupiter.api.Test;

class AdminDeleteAuditTest {

    private final AdminOperationLogService logService = mock(AdminOperationLogService.class);

    @Test
    void contentDeletesWriteAuditLog() {
        SubjectMapper subjectMapper = mock(SubjectMapper.class);
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        QuestionOptionMapper questionOptionMapper = mock(QuestionOptionMapper.class);
        AdminContentController controller = new AdminContentController(
            subjectMapper,
            mock(CourseLevelMapper.class),
            questionMapper,
            questionOptionMapper,
            mock(QuestionAudioService.class),
            logService,
            mock(AiService.class)
        );

        controller.subjectDelete(11L);
        controller.questionDelete(12L);

        verify(subjectMapper).deleteById(11L);
        verify(questionMapper).deleteById(12L);
        verify(logService).write("content", "delete", "subject", 11L, "delete subject");
        verify(logService).write("content", "delete", "question", 12L, "delete question");
    }

    @Test
    void gameDeletesWriteAuditLog() {
        PetMapper petMapper = mock(PetMapper.class);
        AchievementMapper achievementMapper = mock(AchievementMapper.class);
        AdminGameController controller = new AdminGameController(
            petMapper,
            mock(PetEvolutionMapper.class),
            mock(PetItemMapper.class),
            mock(PetDecorationMapper.class),
            achievementMapper,
            mock(AchievementTierMapper.class),
            mock(StickerMapper.class),
            mock(StickerSeriesMapper.class),
            mock(TitleMapper.class),
            logService
        );

        controller.petDelete(21L);
        controller.achievementDelete(22L);

        verify(petMapper).deleteById(21L);
        verify(achievementMapper).deleteById(22L);
        verify(logService).write("game", "delete", "pet", 21L, "delete pet");
        verify(logService).write("game", "delete", "achievement", 22L, "delete achievement");
    }

    @Test
    void dictionaryAndGradeDeletesWriteAuditLog() {
        DictTypeMapper dictTypeMapper = mock(DictTypeMapper.class);
        DictDataMapper dictDataMapper = mock(DictDataMapper.class);
        GradeLevelMapper gradeLevelMapper = mock(GradeLevelMapper.class);
        AdminDictController dictController = new AdminDictController(dictTypeMapper, dictDataMapper, logService);
        AdminGradeLevelController gradeController = new AdminGradeLevelController(gradeLevelMapper, logService);

        dictController.dictTypeDelete(31L);
        dictController.dictDataDelete(32L);
        gradeController.delete(33L);

        verify(dictTypeMapper).deleteById(31L);
        verify(dictDataMapper).deleteById(32L);
        verify(gradeLevelMapper).deleteById(33L);
        verify(logService).write("dict", "delete", "dict-type", 31L, "delete dict type");
        verify(logService).write("dict", "delete", "dict-data", 32L, "delete dict data");
        verify(logService).write("grade-level", "delete", "grade-level", 33L, "delete grade level");
        verify(dictDataMapper).delete(any());
    }
}
