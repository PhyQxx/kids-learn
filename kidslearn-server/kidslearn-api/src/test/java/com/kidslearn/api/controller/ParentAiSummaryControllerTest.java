package com.kidslearn.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kidslearn.api.entity.CourseLevel;
import com.kidslearn.api.entity.DailyStats;
import com.kidslearn.api.entity.Family;
import com.kidslearn.api.entity.FamilyChild;
import com.kidslearn.api.entity.LearningRecord;
import com.kidslearn.api.entity.Subject;
import com.kidslearn.api.entity.TimeControl;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.CourseLevelMapper;
import com.kidslearn.api.mapper.DailyStatsMapper;
import com.kidslearn.api.mapper.FamilyChildMapper;
import com.kidslearn.api.mapper.FamilyMapper;
import com.kidslearn.api.mapper.LearningRecordMapper;
import com.kidslearn.api.mapper.SubjectMapper;
import com.kidslearn.api.mapper.TimeControlMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.api.realtime.RealtimeSessionRegistry;
import com.kidslearn.api.service.AiService;
import com.kidslearn.common.result.R;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ParentAiSummaryControllerTest {

    @Test
    void generatesParentSummaryFromReportOnly() {
        UserMapper userMapper = mock(UserMapper.class);
        FamilyMapper familyMapper = mock(FamilyMapper.class);
        FamilyChildMapper familyChildMapper = mock(FamilyChildMapper.class);
        TimeControlMapper timeControlMapper = mock(TimeControlMapper.class);
        DailyStatsMapper dailyStatsMapper = mock(DailyStatsMapper.class);
        LearningRecordMapper learningRecordMapper = mock(LearningRecordMapper.class);
        CourseLevelMapper courseLevelMapper = mock(CourseLevelMapper.class);
        SubjectMapper subjectMapper = mock(SubjectMapper.class);
        RealtimeSessionRegistry realtimeSessionRegistry = mock(RealtimeSessionRegistry.class);
        AiService aiService = mock(AiService.class);

        DailyStats todayStats = new DailyStats();
        todayStats.setUserId(12L);
        todayStats.setStatDate(LocalDate.now());
        todayStats.setLearnMinutes(25);
        todayStats.setCompletedLevels(2);
        when(dailyStatsMapper.selectList(any())).thenReturn(List.of(todayStats));
        when(dailyStatsMapper.selectOne(any())).thenReturn(todayStats);

        LearningRecord record = new LearningRecord();
        record.setId(90L);
        record.setUserId(12L);
        record.setCourseLevelId(31L);
        record.setScore(80);
        record.setStars(2);
        record.setAnswerTime(600);
        record.setWrongCount(1);
        record.setIsPass(1);
        record.setPlayTime(LocalDateTime.now());
        when(learningRecordMapper.selectList(any())).thenReturn(List.of(record));
        when(learningRecordMapper.selectOne(any())).thenReturn(record);

        Subject subject = new Subject();
        subject.setId(3L);
        subject.setSubjectName("数学");
        when(subjectMapper.selectList(any())).thenReturn(List.of(subject));
        when(subjectMapper.selectById(3L)).thenReturn(subject);

        CourseLevel level = new CourseLevel();
        level.setId(31L);
        level.setSubjectId(3L);
        level.setLevelName("加法练习");
        level.setTotalQuestions(5);
        when(courseLevelMapper.selectList(any())).thenReturn(List.of(level));
        when(courseLevelMapper.selectById(31L)).thenReturn(level);

        Family family = new Family();
        family.setId(5L);
        family.setFamilyCode("ABCD");
        family.setFamilyName("快乐之家");
        family.setParentUserId(12L);
        when(familyMapper.selectOne(any())).thenReturn(family);

        FamilyChild familyChild = new FamilyChild();
        familyChild.setFamilyId(5L);
        familyChild.setChildUserId(22L);
        when(familyChildMapper.selectList(any())).thenReturn(List.of(familyChild));

        User child = new User();
        child.setId(22L);
        child.setNickname("小明");
        when(userMapper.selectById(22L)).thenReturn(child);

        TimeControl timeControl = new TimeControl();
        timeControl.setChildUserId(22L);
        timeControl.setDailyLimit(40);
        timeControl.setIsEnabled(1);
        timeControl.setForbiddenStart(LocalTime.of(8, 0));
        timeControl.setForbiddenEnd(LocalTime.of(21, 0));
        when(timeControlMapper.selectOne(any())).thenReturn(timeControl);
        when(realtimeSessionRegistry.connectedUserIds()).thenReturn(Set.of(22L));

        Map<String, Object> summary = Map.of(
            "summary", "今天学习节奏稳定",
            "highlights", List.of("数学练习完成度不错"),
            "concerns", List.of("接近每日时长上限"),
            "suggestions", List.of("安排一次短休息")
        );
        when(aiService.generateParentSummary(anyMap(), anyMap())).thenReturn(summary);

        ParentController controller = new ParentController(
            userMapper,
            familyMapper,
            familyChildMapper,
            timeControlMapper,
            dailyStatsMapper,
            learningRecordMapper,
            courseLevelMapper,
            subjectMapper,
            realtimeSessionRegistry,
            aiService
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 12L);

        R<Map<String, Object>> response = controller.getAiSummary(request);

        assertEquals(200, response.getCode());
        assertEquals("今天学习节奏稳定", response.getData().get("summary"));
        assertEquals(List.of("安排一次短休息"), response.getData().get("suggestions"));
        verify(aiService).generateParentSummary(
            argThat(report -> ((Map<?, ?>) report.get("today")).get("learnMinutes").equals(25)),
            argThat(Map::isEmpty)
        );
        assertTrue(((List<?>) response.getData().get("highlights")).contains("数学练习完成度不错"));
    }
}
