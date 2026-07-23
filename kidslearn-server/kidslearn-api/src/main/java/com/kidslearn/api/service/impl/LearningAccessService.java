package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.LearningRecord;
import com.kidslearn.api.entity.TimeControl;
import com.kidslearn.api.entity.UserQuestionRecord;
import com.kidslearn.api.mapper.LearningRecordMapper;
import com.kidslearn.api.mapper.TimeControlMapper;
import com.kidslearn.api.mapper.UserQuestionRecordMapper;
import com.kidslearn.common.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Central server-side gate for every learning scene. */
@Service
public class LearningAccessService {

    public enum Scene {
        LEVEL_QUIZ,
        PRACTICE,
        ADAPTIVE,
        ASSESSMENT,
        WRONG_RETRY,
        VIDEO,
        CHALLENGE
    }

    private final TimeControlMapper timeControlMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final UserQuestionRecordMapper userQuestionRecordMapper;
    private final Clock clock;

    @Autowired
    public LearningAccessService(
            TimeControlMapper timeControlMapper,
            LearningRecordMapper learningRecordMapper,
            UserQuestionRecordMapper userQuestionRecordMapper) {
        this(timeControlMapper, learningRecordMapper, userQuestionRecordMapper, Clock.systemDefaultZone());
    }

    LearningAccessService(
            TimeControlMapper timeControlMapper,
            LearningRecordMapper learningRecordMapper,
            UserQuestionRecordMapper userQuestionRecordMapper,
            Clock clock) {
        this.timeControlMapper = timeControlMapper;
        this.learningRecordMapper = learningRecordMapper;
        this.userQuestionRecordMapper = userQuestionRecordMapper;
        this.clock = clock;
    }

    public void checkAccess(Long userId, Scene scene) {
        LearningAccessPolicy.Decision decision = evaluate(userId);
        if (!decision.allowed()) {
            throw new BusinessException(decision.message());
        }
    }

    public Map<String, Object> getAccessStatus(Long userId) {
        LearningAccessPolicy.Decision decision = evaluate(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("allowed", decision.allowed());
        result.put("reasonCode", decision.reasonCode());
        result.put("message", decision.message());
        result.put("usedMinutes", decision.usedMinutes());
        result.put("limitMinutes", decision.limitMinutes());
        result.put("nextAllowedAt", decision.nextAllowedAt());
        return result;
    }

    private LearningAccessPolicy.Decision evaluate(Long userId) {
        TimeControl control = timeControlMapper.selectOne(
            new LambdaQueryWrapper<TimeControl>()
                .eq(TimeControl::getUserId, userId)
                .last("LIMIT 1")
        );
        if (control == null) {
            return LearningAccessPolicy.Decision.allowed(calculateTodayUsageMinutes(userId), null);
        }

        int usedMinutes = calculateTodayUsageMinutes(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        return LearningAccessPolicy.evaluate(
            Integer.valueOf(1).equals(control.getEnabled()),
            Integer.valueOf(1).equals(control.getLimitEnabled()),
            control.getDailyLimitMinutes(),
            Integer.valueOf(1).equals(control.getAllowedWindowEnabled()),
            control.getAllowedStartTime(),
            control.getAllowedEndTime(),
            usedMinutes,
            now
        );
    }

    int calculateTodayUsageMinutes(Long userId) {
        LocalDateTime todayStart = LocalDate.now(clock).atStartOfDay();
        List<LearningRecord> levelRecords = learningRecordMapper.selectList(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .ge(LearningRecord::getCreateTime, todayStart)
        );
        long levelSeconds = levelRecords.stream()
            .map(LearningRecord::getAnswerTime)
            .filter(java.util.Objects::nonNull)
            .mapToLong(value -> Math.max(0, value))
            .sum();

        List<UserQuestionRecord> practiceRecords = userQuestionRecordMapper.selectList(
            new LambdaQueryWrapper<UserQuestionRecord>()
                .eq(UserQuestionRecord::getUserId, userId)
                .ge(UserQuestionRecord::getCreateTime, todayStart)
        );
        long practiceMillis = practiceRecords.stream()
            .map(UserQuestionRecord::getAnswerTimeMs)
            .filter(java.util.Objects::nonNull)
            .mapToLong(value -> Math.max(0, value))
            .sum();

        long totalSeconds = levelSeconds + (practiceMillis / 1000L);
        return (int) Math.min(Integer.MAX_VALUE, totalSeconds / 60L);
    }
}
