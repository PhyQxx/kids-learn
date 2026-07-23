package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kidslearn.api.dto.learn.PracticeModeVO;
import com.kidslearn.api.entity.PracticeMode;
import com.kidslearn.api.entity.Question;
import com.kidslearn.api.mapper.*;
import com.kidslearn.api.realtime.RealtimeEventPublisher;
import com.kidslearn.api.service.AchievementService;
import com.kidslearn.api.service.AiService;
import com.kidslearn.api.service.PetService;
import com.kidslearn.api.service.EntitlementService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LearnServiceImplPracticeModeTest {

    @Test
    void getPracticeModesReturnsConfiguredModes() {
        PracticeModeMapper practiceModeMapper = mock(PracticeModeMapper.class);
        LearnServiceImpl service = serviceWithPracticeModeMapper(practiceModeMapper);
        PracticeMode mode = practiceMode(7L, 2L, "20以内加法", "TIMED", 60);
        when(practiceModeMapper.selectList(any())).thenReturn(List.of(mode));

        List<PracticeModeVO> modes = service.getPracticeModes(1L, 2L);

        assertEquals(1, modes.size());
        assertEquals(7L, modes.get(0).getId());
        assertEquals("20以内加法", modes.get(0).getName());
        assertEquals("TIMED", modes.get(0).getType());
        assertEquals(60, modes.get(0).getTimeLimitSeconds());
    }

    @Test
    void startPracticeUsesConfiguredModeMetadata() {
        PracticeModeMapper practiceModeMapper = mock(PracticeModeMapper.class);
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        LearnServiceImpl service = serviceWithPracticeModeMapperAndQuestionMapper(practiceModeMapper, questionMapper);
        PracticeMode mode = practiceMode(7L, 2L, "20以内加法", "TIMED", 60);
        when(practiceModeMapper.selectById(7L)).thenReturn(mode);

        Question question = new Question();
        question.setId(99L);
        question.setSubjectId(2L);
        question.setQuestionContent("1 + 1 = ?");
        when(questionMapper.selectList(any())).thenReturn(List.of(question));

        Map<String, Object> result = service.startPractice(1L, 7L);

        assertEquals(7L, result.get("modeId"));
        assertEquals("20以内加法", result.get("modeName"));
        assertEquals("TIMED", result.get("type"));
        assertEquals(60, result.get("timeLimit"));
    }

    private static PracticeMode practiceMode(Long id, Long subjectId, String name, String type, Integer timeLimitSeconds) {
        PracticeMode mode = new PracticeMode();
        mode.setId(id);
        mode.setSubjectId(subjectId);
        mode.setName(name);
        mode.setDescription("练习描述");
        mode.setIcon("➕");
        mode.setType(type);
        mode.setTimeLimitSeconds(timeLimitSeconds);
        mode.setTags("HOT");
        mode.setStatus(1);
        return mode;
    }

    private static LearnServiceImpl serviceWithPracticeModeMapper(PracticeModeMapper practiceModeMapper) {
        return serviceWithPracticeModeMapperAndQuestionMapper(practiceModeMapper, mock(QuestionMapper.class));
    }

    private static LearnServiceImpl serviceWithPracticeModeMapperAndQuestionMapper(
            PracticeModeMapper practiceModeMapper,
            QuestionMapper questionMapper) {
        return new LearnServiceImpl(
            mock(SubjectMapper.class),
            mock(CourseVideoMapper.class),
            mock(CourseLevelMapper.class),
            questionMapper,
            mock(QuestionOptionMapper.class),
            mock(LearningRecordMapper.class),
            mock(UserVideoProgressMapper.class),
            mock(WrongTopicMapper.class),
            mock(DailyStatsMapper.class),
            mock(UserMapper.class),
            mock(FamilyMapper.class),
            mock(FamilyChildMapper.class),
            mock(ChildProfileMapper.class),
            mock(StickerMapper.class),
            mock(RewardLogMapper.class),
            mock(UserStickerMapper.class),
            mock(GradeLevelMapper.class),
            mock(DailyCheckinMapper.class),
            mock(AchievementService.class),
            mock(RealtimeEventPublisher.class),
            mock(PetService.class),
            mock(AiService.class),
            practiceModeMapper,
            mock(EntitlementService.class),
            mock(LearningAccessService.class),
            mock(PracticeSessionMapper.class),
            mock(UserQuestionRecordMapper.class)
        );
    }
}
