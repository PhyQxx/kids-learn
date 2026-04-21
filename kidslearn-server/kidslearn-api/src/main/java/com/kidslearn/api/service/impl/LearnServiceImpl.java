package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidslearn.api.dto.learn.DailyTaskVO;
import com.kidslearn.api.dto.learn.LevelResultVO;
import com.kidslearn.api.dto.learn.SubmitAnswerDTO;
import com.kidslearn.api.entity.*;
import com.kidslearn.api.mapper.*;
import com.kidslearn.api.service.LearnService;
import com.kidslearn.common.exception.BusinessException;
import com.kidslearn.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearnServiceImpl implements LearnService {

    private final SubjectMapper subjectMapper;
    private final CourseMapper courseMapper;
    private final CourseLevelMapper courseLevelMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final WrongTopicMapper wrongTopicMapper;
    private final DailyStatsMapper dailyStatsMapper;
    private final UserMapper userMapper;
    private final ChildProfileMapper childProfileMapper;
    private final StickerMapper stickerMapper;
    private final RewardLogMapper rewardLogMapper;
    private final UserStickerMapper userStickerMapper;

    @Override
    public DailyTaskVO getDailyTasks(Long userId) {
        DailyTaskVO vo = new DailyTaskVO();
        vo.setDate(LocalDate.now().toString());
        vo.setTotalTime(30); // target 30 mins per day

        List<Subject> subjects = subjectMapper.selectList(
            new LambdaQueryWrapper<Subject>().eq(Subject::getStatus, 1).orderByAsc(Subject::getSortOrder)
        );

        // get today's learning records
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<LearningRecord> todayRecords = learningRecordMapper.selectList(
            new LambdaQueryWrapper<LearningRecord>().eq(LearningRecord::getUserId, userId).ge(LearningRecord::getCreateTime, todayStart)
        );

        List<DailyTaskVO.TaskItemVO> tasks = new ArrayList<>();
        for (Subject subject : subjects) {
            DailyTaskVO.TaskItemVO task = new DailyTaskVO.TaskItemVO();
            task.setSubject(subject.getSubjectCode());
            task.setSubjectName(subject.getSubjectName());
            task.setSubjectIcon(subject.getIconUrl());
            task.setTargetMinutes(5);

            // calculate today minutes for this subject
            Set<Long> courseIds = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().eq(Course::getSubjectId, subject.getId())
            ).stream().map(Course::getId).collect(Collectors.toSet());

            Set<Long> levelIds = courseLevelMapper.selectList(
                new LambdaQueryWrapper<CourseLevel>().in(CourseLevel::getCourseId, courseIds)
            ).stream().map(CourseLevel::getId).collect(Collectors.toSet());

            int todayMins = todayRecords.stream()
                .filter(r -> levelIds.contains(r.getCourseLevelId()))
                .mapToInt(r -> Math.max(1, r.getAnswerTime() / 60))
                .sum();

            task.setTodayMinutes(todayMins);
            int progress = task.getTargetMinutes() > 0 ? Math.min(100, todayMins * 100 / task.getTargetMinutes()) : 0;
            task.setProgress(progress);
            task.setStatus(progress >= 100 ? "COMPLETED" : progress > 0 ? "IN_PROGRESS" : "NOT_STARTED");
            tasks.add(task);
        }
        vo.setTasks(tasks);
        return vo;
    }

    @Override
    public List<Map<String, Object>> getSubjects(Long userId) {
        List<Subject> subjects = subjectMapper.selectList(
            new LambdaQueryWrapper<Subject>().eq(Subject::getStatus, 1).orderByAsc(Subject::getSortOrder)
        );
        return subjects.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("code", s.getSubjectCode());
            map.put("name", s.getSubjectName());
            map.put("icon", s.getIconUrl());
            map.put("color", s.getColor());
            // count courses
            Long courseCount = courseMapper.selectCount(
                new LambdaQueryWrapper<Course>().eq(Course::getSubjectId, s.getId()).eq(Course::getStatus, 1)
            );
            map.put("courseCount", courseCount);
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public PageResult<Map<String, Object>> getCourses(Long userId, Long subjectId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
            .eq(Course::getStatus, 1)
            .eq(subjectId != null, Course::getSubjectId, subjectId)
            .orderByAsc(Course::getSortOrder);

        Page<Course> coursePage = courseMapper.selectPage(new Page<>(page, pageSize), wrapper);

        List<Map<String, Object>> list = coursePage.getRecords().stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("subjectId", c.getSubjectId());
            map.put("courseName", c.getCourseName());
            map.put("courseDesc", c.getCourseDesc());
            map.put("coverUrl", c.getCoverUrl());
            map.put("totalLevels", c.getTotalLevels());
            map.put("difficulty", c.getDifficulty());
            map.put("isElite", c.getIsElite());

            // user progress
            Long completedCount = learningRecordMapper.selectCount(
                new LambdaQueryWrapper<LearningRecord>()
                    .eq(LearningRecord::getUserId, userId)
                    .eq(LearningRecord::getIsPass, 1)
                    .inSql(LearningRecord::getCourseLevelId,
                        "SELECT id FROM course_level WHERE course_id = " + c.getId())
            );
            map.put("completedLevels", completedCount);

            // total stars - get best star for each level in this course
            List<LearningRecord> allRecords = learningRecordMapper.selectList(
                new LambdaQueryWrapper<LearningRecord>()
                    .eq(LearningRecord::getUserId, userId)
                    .inSql(LearningRecord::getCourseLevelId,
                        "SELECT id FROM course_level WHERE course_id = " + c.getId())
            );
            // group by level, take max stars per level
            Map<Long, Integer> bestStarsPerLevel = new HashMap<>();
            for (LearningRecord r : allRecords) {
                bestStarsPerLevel.merge(r.getCourseLevelId(), r.getStars(), Math::max);
            }
            int totalStars = bestStarsPerLevel.values().stream().mapToInt(Integer::intValue).sum();
            map.put("totalStars", totalStars);

            return map;
        }).collect(Collectors.toList());

        return new PageResult<>(list, coursePage.getTotal(), page, pageSize);
    }

    @Override
    public List<Map<String, Object>> getLevels(Long userId, Long courseId) {
        List<CourseLevel> levels = courseLevelMapper.selectList(
            new LambdaQueryWrapper<CourseLevel>()
                .eq(CourseLevel::getCourseId, courseId)
                .eq(CourseLevel::getStatus, 1)
                .orderByAsc(CourseLevel::getLevelNum)
        );

        // first level always unlocked
        boolean prevPassed = true;

        List<Map<String, Object>> result = new ArrayList<>();
        for (CourseLevel level : levels) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", level.getId());
            map.put("levelNum", level.getLevelNum());
            map.put("levelName", level.getLevelName());
            map.put("levelDesc", level.getLevelDesc());
            map.put("coverUrl", level.getCoverUrl());
            map.put("totalQuestions", level.getTotalQuestions());
            map.put("passScore", level.getPassScore());
            map.put("expReward", level.getExpReward());
            map.put("goldReward", level.getGoldReward());

            boolean isUnlock = level.getLevelNum() == 1 || prevPassed || level.getIsUnlock() == 1;
            map.put("isUnlock", isUnlock);

            // user best record
            LearningRecord bestRecord = learningRecordMapper.selectOne(
                new LambdaQueryWrapper<LearningRecord>()
                    .eq(LearningRecord::getUserId, userId)
                    .eq(LearningRecord::getCourseLevelId, level.getId())
                    .orderByDesc(LearningRecord::getStars)
                    .last("LIMIT 1")
            );
            map.put("myStars", bestRecord != null ? bestRecord.getStars() : 0);
            map.put("isPassed", bestRecord != null && bestRecord.getIsPass() == 1);

            prevPassed = bestRecord != null && bestRecord.getIsPass() == 1;
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getQuestions(Long levelId) {
        List<Question> questions = questionMapper.selectList(
            new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseLevelId, levelId)
                .orderByAsc(Question::getSortOrder)
        );

        return questions.stream().map(q -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", q.getId());
            map.put("questionType", q.getQuestionType());
            map.put("questionContent", q.getQuestionContent());
            map.put("difficulty", q.getDifficulty());
            map.put("score", q.getScore());
            map.put("timeLimit", q.getTimeLimit());

            List<QuestionOption> options = questionOptionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>()
                    .eq(QuestionOption::getQuestionId, q.getId())
                    .orderByAsc(QuestionOption::getSortOrder)
            );
            List<Map<String, String>> optionList = options.stream().map(o -> {
                Map<String, String> opt = new HashMap<>();
                opt.put("optionLabel", o.getOptionLabel());
                opt.put("optionContent", o.getOptionContent());
                return opt;
            }).collect(Collectors.toList());
            map.put("options", optionList);
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> submitAnswer(Long userId, SubmitAnswerDTO dto) {
        Question question = questionMapper.selectById(dto.getQuestionId());
        if (question == null) {
            throw new BusinessException("题目不存在");
        }

        QuestionOption correctOption = questionOptionMapper.selectOne(
            new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, dto.getQuestionId())
                .eq(QuestionOption::getIsCorrect, 1)
                .last("LIMIT 1")
        );

        String correctAnswer = correctOption != null ? correctOption.getOptionLabel() : "";
        boolean isCorrect = correctAnswer.equalsIgnoreCase(dto.getAnswer());

        Map<String, Object> result = new HashMap<>();
        result.put("correct", isCorrect);
        result.put("correctAnswer", correctAnswer);
        result.put("explanation", question.getAnalysis());

        if (isCorrect) {
            result.put("gold", 5);
            result.put("exp", 5);
        } else {
            // save to wrong topic
            saveWrongTopic(userId, dto.getQuestionId(), dto.getAnswer(), correctAnswer);
            result.put("gold", 0);
            result.put("exp", 0);
        }

        return result;
    }

    @Override
    @Transactional
    public LevelResultVO completeLevel(Long userId, Long levelId, Integer totalScore, Integer totalTime, Integer wrongCount) {
        CourseLevel level = courseLevelMapper.selectById(levelId);
        if (level == null) {
            throw new BusinessException("关卡不存在");
        }

        // calculate stars
        int stars = calculateStars(totalScore, level.getStarThresholds());
        boolean isPass = totalScore >= level.getPassScore();

        // save learning record
        LearningRecord record = new LearningRecord();
        record.setUserId(userId);
        record.setCourseLevelId(levelId);
        record.setScore(totalScore);
        record.setStars(stars);
        record.setAnswerTime(totalTime);
        record.setWrongCount(wrongCount);
        record.setIsPass(isPass ? 1 : 0);
        record.setPlayTime(LocalDateTime.now());
        learningRecordMapper.insert(record);

        // build result
        LevelResultVO vo = new LevelResultVO();
        vo.setScore(totalScore);
        vo.setStars(stars);
        vo.setWrongCount(wrongCount);
        vo.setIsPass(isPass);

        if (isPass) {
            // award gold and exp
            int goldReward = level.getGoldReward() + (stars == 3 ? 10 : 0);
            int expReward = level.getExpReward();
            vo.setGold(goldReward);
            vo.setExp(expReward);

            User user = userMapper.selectById(userId);
            user.setGold(user.getGold() + goldReward);
            user.setTotalExp(user.getTotalExp() + expReward);
            user.setLevel(calculateLevel(user.getTotalExp()));
            userMapper.updateById(user);

            // reward log
            RewardLog goldLog = new RewardLog();
            goldLog.setUserId(userId);
            goldLog.setRewardType(1); // gold
            goldLog.setQuantity(goldReward);
            goldLog.setSourceType("COMPLETE_LEVEL");
            goldLog.setSourceId(levelId);
            goldLog.setDescription("完成关卡: " + level.getLevelName());
            rewardLogMapper.insert(goldLog);

            RewardLog expLog = new RewardLog();
            expLog.setUserId(userId);
            expLog.setRewardType(2); // exp
            expLog.setQuantity(expReward);
            expLog.setSourceType("COMPLETE_LEVEL");
            expLog.setSourceId(levelId);
            rewardLogMapper.insert(expLog);

            // sticker reward
            if (level.getStickerId() != null) {
                Sticker sticker = stickerMapper.selectById(level.getStickerId());
                if (sticker != null) {
                    UserSticker existing = userStickerMapper.selectOne(
                        new LambdaQueryWrapper<UserSticker>()
                            .eq(UserSticker::getUserId, userId)
                            .eq(UserSticker::getStickerId, sticker.getId())
                    );
                    if (existing != null) {
                        existing.setQuantity(existing.getQuantity() + 1);
                        userStickerMapper.updateById(existing);
                    } else {
                        UserSticker us = new UserSticker();
                        us.setUserId(userId);
                        us.setStickerId(sticker.getId());
                        us.setQuantity(1);
                        userStickerMapper.insert(us);
                    }
                    vo.setStickerId(sticker.getId());
                    vo.setStickerName(sticker.getStickerName());
                    vo.setStickerUrl(sticker.getStickerUrl());
                }
            }

            // unlock next level
            boolean unlockedNext = unlockNextLevel(level);
            vo.setUnlockedNextLevel(unlockedNext);

            // update daily stats
            updateDailyStats(userId, totalTime, 1, goldReward, expReward);
        } else {
            vo.setGold(0);
            vo.setExp(0);
            vo.setUnlockedNextLevel(false);
        }

        return vo;
    }

    private int calculateStars(Integer score, String starThresholds) {
        if (starThresholds == null) return score >= 60 ? 1 : 0;
        String[] thresholds = starThresholds.split(",");
        if (thresholds.length >= 3 && score >= Integer.parseInt(thresholds[2].trim())) return 3;
        if (thresholds.length >= 2 && score >= Integer.parseInt(thresholds[1].trim())) return 2;
        if (thresholds.length >= 1 && score >= Integer.parseInt(thresholds[0].trim())) return 1;
        return 0;
    }

    private int calculateLevel(Integer totalExp) {
        if (totalExp < 100) return 1;
        if (totalExp < 300) return 2;
        if (totalExp < 600) return 3;
        if (totalExp < 1000) return 4;
        if (totalExp < 1500) return 5;
        if (totalExp < 2100) return 6;
        if (totalExp < 2800) return 7;
        if (totalExp < 3600) return 8;
        if (totalExp < 4500) return 9;
        if (totalExp < 5500) return 10;
        // beyond level 10, every 1200 exp = 1 level
        return 10 + (totalExp - 5500) / 1200 + 1;
    }

    private boolean unlockNextLevel(CourseLevel currentLevel) {
        CourseLevel nextLevel = courseLevelMapper.selectOne(
            new LambdaQueryWrapper<CourseLevel>()
                .eq(CourseLevel::getCourseId, currentLevel.getCourseId())
                .eq(CourseLevel::getLevelNum, currentLevel.getLevelNum() + 1)
                .last("LIMIT 1")
        );
        if (nextLevel != null && nextLevel.getIsUnlock() == 0) {
            nextLevel.setIsUnlock(1);
            courseLevelMapper.updateById(nextLevel);
            return true;
        }
        return false;
    }

    private void saveWrongTopic(Long userId, Long questionId, String wrongAnswer, String correctAnswer) {
        WrongTopic existing = wrongTopicMapper.selectOne(
            new LambdaQueryWrapper<WrongTopic>()
                .eq(WrongTopic::getUserId, userId)
                .eq(WrongTopic::getQuestionId, questionId)
        );
        if (existing != null) {
            existing.setTimes(existing.getTimes() + 1);
            existing.setWrongAnswer(wrongAnswer);
            existing.setCorrectAnswer(correctAnswer);
            existing.setLastWrongTime(LocalDateTime.now());
            existing.setIsMastered(0);
            wrongTopicMapper.updateById(existing);
        } else {
            WrongTopic wt = new WrongTopic();
            wt.setUserId(userId);
            wt.setQuestionId(questionId);
            wt.setWrongAnswer(wrongAnswer);
            wt.setCorrectAnswer(correctAnswer);
            wt.setTimes(1);
            wt.setLastWrongTime(LocalDateTime.now());
            wt.setIsMastered(0);
            wrongTopicMapper.insert(wt);
        }
    }

    @Override
    public List<Map<String, Object>> getLearningRecords(Long userId, String date) {
        LambdaQueryWrapper<LearningRecord> wrapper = new LambdaQueryWrapper<LearningRecord>()
            .eq(LearningRecord::getUserId, userId)
            .orderByDesc(LearningRecord::getCreateTime);
        if (date != null && !date.isEmpty()) {
            LocalDate filterDate = LocalDate.parse(date);
            wrapper.ge(LearningRecord::getCreateTime, filterDate.atStartOfDay())
                   .lt(LearningRecord::getCreateTime, filterDate.plusDays(1).atStartOfDay());
        }
        List<LearningRecord> records = learningRecordMapper.selectList(wrapper);
        return records.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("courseLevelId", r.getCourseLevelId());
            map.put("score", r.getScore());
            map.put("stars", r.getStars());
            map.put("timeCost", r.getAnswerTime());
            map.put("wrongCount", r.getWrongCount());
            map.put("isPass", r.getIsPass());
            map.put("createTime", r.getCreateTime() != null ? r.getCreateTime().toString() : null);
            // lookup level name
            CourseLevel level = courseLevelMapper.selectById(r.getCourseLevelId());
            if (level != null) {
                map.put("levelName", level.getLevelName());
                Course course = courseMapper.selectById(level.getCourseId());
                if (course != null) {
                    map.put("courseName", course.getCourseName());
                    Subject subject = subjectMapper.selectById(course.getSubjectId());
                    if (subject != null) map.put("subjectName", subject.getSubjectName());
                }
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getWrongTopics(Long userId) {
        List<WrongTopic> topics = wrongTopicMapper.selectList(
            new LambdaQueryWrapper<WrongTopic>()
                .eq(WrongTopic::getUserId, userId)
                .eq(WrongTopic::getIsMastered, 0)
                .orderByDesc(WrongTopic::getLastWrongTime)
        );
        return topics.stream().map(wt -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", wt.getId());
            map.put("questionId", wt.getQuestionId());
            map.put("userAnswer", wt.getWrongAnswer());
            map.put("correctAnswer", wt.getCorrectAnswer());
            map.put("wrongCount", wt.getTimes());
            // lookup question content
            Question q = questionMapper.selectById(wt.getQuestionId());
            if (q != null) {
                map.put("questionContent", q.getQuestionContent());
                map.put("levelId", q.getCourseLevelId());
                CourseLevel level = courseLevelMapper.selectById(q.getCourseLevelId());
                if (level != null) {
                    map.put("levelName", level.getLevelName());
                    Course course = courseMapper.selectById(level.getCourseId());
                    if (course != null) {
                        Subject subject = subjectMapper.selectById(course.getSubjectId());
                        if (subject != null) map.put("subjectName", subject.getSubjectName());
                    }
                }
            }
            return map;
        }).collect(Collectors.toList());
    }

    private void updateDailyStats(Long userId, Integer answerTimeSeconds, Integer completedLevels, Integer gold, Integer exp) {
        LocalDate today = LocalDate.now();
        DailyStats stats = dailyStatsMapper.selectOne(
            new LambdaQueryWrapper<DailyStats>()
                .eq(DailyStats::getUserId, userId)
                .eq(DailyStats::getStatDate, today)
        );
        if (stats != null) {
            stats.setLearnMinutes(stats.getLearnMinutes() + Math.max(1, answerTimeSeconds / 60));
            stats.setCompletedLevels(stats.getCompletedLevels() + completedLevels);
            stats.setEarnedGold(stats.getEarnedGold() + gold);
            stats.setEarnedExp(stats.getEarnedExp() + exp);
            dailyStatsMapper.updateById(stats);
        } else {
            stats = new DailyStats();
            stats.setUserId(userId);
            stats.setStatDate(today);
            stats.setLearnMinutes(Math.max(1, answerTimeSeconds / 60));
            stats.setCompletedLevels(completedLevels);
            stats.setEarnedGold(gold);
            stats.setEarnedExp(exp);
            stats.setLoginCount(1);
            dailyStatsMapper.insert(stats);
        }
    }
}
