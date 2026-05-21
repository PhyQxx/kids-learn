package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.dto.learn.*;
import com.kidslearn.api.entity.*;
import com.kidslearn.api.mapper.*;
import com.kidslearn.api.realtime.RealtimeEventPublisher;
import com.kidslearn.api.service.AchievementService;
import com.kidslearn.api.service.AiService;
import com.kidslearn.api.service.LearnService;
import com.kidslearn.api.service.PetService;
import com.kidslearn.common.exception.BusinessException;
import com.kidslearn.common.util.RichContentUtil;
import lombok.RequiredArgsConstructor;
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
    private final CourseVideoMapper courseVideoMapper;
    private final CourseLevelMapper courseLevelMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final UserVideoProgressMapper userVideoProgressMapper;
    private final WrongTopicMapper wrongTopicMapper;
    private final DailyStatsMapper dailyStatsMapper;
    private final UserMapper userMapper;
    private final FamilyMapper familyMapper;
    private final FamilyChildMapper familyChildMapper;
    private final ChildProfileMapper childProfileMapper;
    private final StickerMapper stickerMapper;
    private final RewardLogMapper rewardLogMapper;
    private final UserStickerMapper userStickerMapper;
    private final GradeLevelMapper gradeLevelMapper;
    private final DailyCheckinMapper dailyCheckinMapper;
    private final AchievementService achievementService;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final PetService petService;
    private final AiService aiService;
    private final PracticeModeMapper practiceModeMapper;

    @Override
    public DailyTaskVO getDailyTasks(Long userId) {
        DailyTaskVO vo = new DailyTaskVO();
        vo.setDate(LocalDate.now().toString());
        vo.setTotalTime(30);

        List<Subject> subjects = subjectMapper.selectList(
            new LambdaQueryWrapper<Subject>().eq(Subject::getStatus, 1).orderByAsc(Subject::getSortOrder)
        );

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

            Set<Long> levelIds = courseLevelMapper.selectList(
                new LambdaQueryWrapper<CourseLevel>()
                    .eq(CourseLevel::getSubjectId, subject.getId())
                    .eq(CourseLevel::getStatus, 1)
            ).stream().map(CourseLevel::getId).collect(Collectors.toSet());

            if (levelIds.isEmpty()) {
                continue;
            }

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
    public List<Map<String, Object>> getSubjects(Long userId, Long gradeLevelId) {
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

            Long levelCount = courseLevelMapper.selectCount(
                new LambdaQueryWrapper<CourseLevel>()
                    .eq(CourseLevel::getSubjectId, s.getId())
                    .eq(CourseLevel::getStatus, 1)
            );
            map.put("levelCount", levelCount);
            map.put("_hasLevels", levelCount > 0);

            Long totalLevels = courseLevelMapper.selectCount(
                new LambdaQueryWrapper<CourseLevel>()
                    .eq(CourseLevel::getSubjectId, s.getId())
                    .eq(CourseLevel::getStatus, 1)
            );
            if (totalLevels > 0) {
                Long completedLevels = learningRecordMapper.selectCount(
                    new LambdaQueryWrapper<LearningRecord>()
                        .eq(LearningRecord::getUserId, userId)
                        .eq(LearningRecord::getIsPass, 1)
                        .inSql(LearningRecord::getCourseLevelId,
                            "SELECT id FROM course_level WHERE subject_id = " + s.getId() + " AND status = 1")
                );
                int progress = (int) (completedLevels * 100 / totalLevels);
                map.put("progress", progress);
            }

            return map;
        }).filter(m -> (boolean) m.get("_hasLevels")).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getCourseVideos(Long userId, Long courseId) {
        List<CourseVideo> videos = courseVideoMapper.selectList(
            new LambdaQueryWrapper<CourseVideo>()
                .eq(CourseVideo::getCourseId, courseId)
                .eq(CourseVideo::getStatus, 1)
                .orderByAsc(CourseVideo::getSortOrder)
        );
        if (videos.isEmpty()) {
            return List.of();
        }

        List<Long> videoIds = videos.stream().map(CourseVideo::getId).collect(Collectors.toList());
        Map<Long, UserVideoProgress> progressByVideoId = userVideoProgressMapper.selectList(
            new LambdaQueryWrapper<UserVideoProgress>()
                .eq(UserVideoProgress::getUserId, userId)
                .in(UserVideoProgress::getVideoId, videoIds)
        ).stream().collect(Collectors.toMap(UserVideoProgress::getVideoId, p -> p, (left, right) -> left));

        return videos.stream()
            .map(video -> toVideoMap(video, progressByVideoId.get(video.getId())))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> submitVideoProgress(Long userId, SubmitVideoProgressDTO dto) {
        if (dto == null || dto.getVideoId() == null) {
            throw new BusinessException("Video id is required");
        }
        CourseVideo video = courseVideoMapper.selectById(dto.getVideoId());
        if (video == null || video.getStatus() == null || video.getStatus() != 1) {
            throw new BusinessException("Video not found");
        }

        UserVideoProgress record = userVideoProgressMapper.selectOne(
            new LambdaQueryWrapper<UserVideoProgress>()
                .eq(UserVideoProgress::getUserId, userId)
                .eq(UserVideoProgress::getVideoId, dto.getVideoId())
                .last("LIMIT 1")
        );

        int durationSeconds = maxPositive(dto.getDurationSeconds(), video.getDurationSeconds());
        int progressSeconds = dto.getProgressSeconds() == null ? 0 : dto.getProgressSeconds();
        if (record != null) {
            durationSeconds = maxPositive(durationSeconds, record.getDurationSeconds());
            progressSeconds = Math.max(progressSeconds, record.getProgressSeconds() == null ? 0 : record.getProgressSeconds());
        }

        VideoProgressEngine.Progress progress = VideoProgressEngine.evaluate(progressSeconds, durationSeconds);
        boolean completed = progress.completed() || (record != null && Integer.valueOf(1).equals(record.getCompleted()));

        if (record == null) {
            record = new UserVideoProgress();
            record.setUserId(userId);
            record.setVideoId(dto.getVideoId());
            record.setProgressSeconds(progress.progressSeconds());
            record.setDurationSeconds(progress.durationSeconds());
            record.setProgressPercent(progress.progressPercent());
            record.setCompleted(completed ? 1 : 0);
            record.setLastWatchTime(LocalDateTime.now());
            userVideoProgressMapper.insert(record);
        } else {
            record.setProgressSeconds(progress.progressSeconds());
            record.setDurationSeconds(progress.durationSeconds());
            record.setProgressPercent(progress.progressPercent());
            record.setCompleted(completed ? 1 : 0);
            record.setLastWatchTime(LocalDateTime.now());
            userVideoProgressMapper.updateById(record);
        }

        return toVideoProgressMap(record);
    }

    private Map<String, Object> toVideoMap(CourseVideo video, UserVideoProgress savedProgress) {
        int durationSeconds = maxPositive(video.getDurationSeconds(), savedProgress == null ? null : savedProgress.getDurationSeconds());
        VideoProgressEngine.Progress progress = VideoProgressEngine.evaluate(
            savedProgress == null ? 0 : savedProgress.getProgressSeconds(),
            durationSeconds
        );
        boolean completed = progress.completed() || (savedProgress != null && Integer.valueOf(1).equals(savedProgress.getCompleted()));

        Map<String, Object> map = new HashMap<>();
        map.put("id", video.getId());
        map.put("courseId", video.getCourseId());
        map.put("courseLevelId", video.getCourseLevelId());
        map.put("title", video.getTitle());
        map.put("description", video.getDescription());
        map.put("coverUrl", video.getCoverUrl());
        map.put("videoUrl", video.getVideoUrl());
        map.put("durationSeconds", durationSeconds);
        map.put("sortOrder", video.getSortOrder());
        map.put("isFree", video.getIsFree());
        map.put("progressSeconds", progress.progressSeconds());
        map.put("progressPercent", progress.progressPercent());
        map.put("completed", completed);
        return map;
    }

    private Map<String, Object> toVideoProgressMap(UserVideoProgress record) {
        Map<String, Object> map = new HashMap<>();
        map.put("videoId", record.getVideoId());
        map.put("progressSeconds", record.getProgressSeconds());
        map.put("durationSeconds", record.getDurationSeconds());
        map.put("progressPercent", record.getProgressPercent());
        map.put("completed", Integer.valueOf(1).equals(record.getCompleted()));
        return map;
    }

    private int maxPositive(Integer left, Integer right) {
        return Math.max(left == null ? 0 : Math.max(0, left), right == null ? 0 : Math.max(0, right));
    }

    @Override
    public List<Map<String, Object>> getLevels(Long userId, Long subjectId) {
        List<CourseLevel> levels = courseLevelMapper.selectList(
            new LambdaQueryWrapper<CourseLevel>()
                .eq(CourseLevel::getSubjectId, subjectId)
                .eq(CourseLevel::getStatus, 1)
                .orderByAsc(CourseLevel::getLevelNum)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < levels.size(); i++) {
            CourseLevel level = levels.get(i);
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

            LearningRecord bestRecord = learningRecordMapper.selectOne(
                new LambdaQueryWrapper<LearningRecord>()
                    .eq(LearningRecord::getUserId, userId)
                    .eq(LearningRecord::getCourseLevelId, level.getId())
                    .orderByDesc(LearningRecord::getStars)
                    .last("LIMIT 1")
            );
            int myStars = bestRecord != null ? bestRecord.getStars() : 0;
            boolean isPassed = bestRecord != null && bestRecord.getIsPass() == 1;
            map.put("myStars", myStars);
            map.put("isPassed", isPassed);

            boolean isUnlock = level.getIsUnlock() == 1;
            if (!isUnlock && i > 0) {
                CourseLevel prevLevel = levels.get(i - 1);
                LearningRecord prevRecord = learningRecordMapper.selectOne(
                    new LambdaQueryWrapper<LearningRecord>()
                        .eq(LearningRecord::getUserId, userId)
                        .eq(LearningRecord::getCourseLevelId, prevLevel.getId())
                        .orderByDesc(LearningRecord::getStars)
                        .last("LIMIT 1")
                );
                if (prevRecord != null && prevRecord.getIsPass() == 1) {
                    String unlockCondition = level.getUnlockCondition();
                    if (unlockCondition != null && unlockCondition.contains("minStars")) {
                        try {
                            if (unlockCondition.contains("\"" + prevLevel.getId() + "\"")) {
                                int minStars = extractMinStars(unlockCondition);
                                isUnlock = prevRecord.getStars() >= minStars;
                            } else {
                                isUnlock = false;
                            }
                        } catch (Exception e) {
                            isUnlock = true;
                        }
                    } else {
                        isUnlock = true;
                    }
                } else {
                    isUnlock = false;
                }
            }
            map.put("isUnlock", isUnlock);
            result.add(map);
        }
        return result;
    }

    private int extractMinStars(String unlockCondition) {
        try {
            String minStarsPart = unlockCondition.substring(unlockCondition.indexOf("minStars") + 9);
            int minStars = Integer.parseInt(minStarsPart.split("[,}\\[\\]]")[0]);
            return minStars;
        } catch (Exception e) {
            return 1;
        }
    }

    @Override
    public List<Map<String, Object>> getQuestions(Long userId, Long levelId) {
        CourseLevel level = courseLevelMapper.selectById(levelId);
        if (level == null || level.getSubjectId() == null) {
            return List.of();
        }

        Long subjectId = level.getSubjectId();
        int baseCount = level.getBaseQuestionCount() != null ? level.getBaseQuestionCount() : 8;
        int advancedCount = level.getAdvancedQuestionCount() != null ? level.getAdvancedQuestionCount() : 2;

        Long userGradeLevelId = null;
        try {
            ChildProfile profile = childProfileMapper.selectOne(
                new LambdaQueryWrapper<ChildProfile>()
                    .eq(ChildProfile::getUserId, userId)
                    .last("LIMIT 1")
            );
            if (profile != null && profile.getGradeLevel() != null) {
                userGradeLevelId = profile.getGradeLevel().longValue();
            }
        } catch (Exception e) {
            // ignore
        }

        Random random = new Random();
        List<Question> selectedQuestions = new ArrayList<>();

        if (userGradeLevelId != null) {
            LambdaQueryWrapper<Question> baseWrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getSubjectId, subjectId)
                .eq(Question::getGradeLevelId, userGradeLevelId);
            List<Question> baseQuestions = questionMapper.selectList(baseWrapper);
            Collections.shuffle(baseQuestions, random);
            int limit = Math.min(baseCount, baseQuestions.size());
            for (int i = 0; i < limit; i++) {
                selectedQuestions.add(baseQuestions.get(i));
            }

            Long nextGradeLevelId = userGradeLevelId + 1;
            LambdaQueryWrapper<Question> advancedWrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getSubjectId, subjectId)
                .eq(Question::getGradeLevelId, nextGradeLevelId);
            List<Question> advancedQuestions = questionMapper.selectList(advancedWrapper);
            Collections.shuffle(advancedQuestions, random);
            int advLimit = Math.min(advancedCount, advancedQuestions.size());
            for (int i = 0; i < advLimit; i++) {
                selectedQuestions.add(advancedQuestions.get(i));
            }
        }

        if (selectedQuestions.isEmpty()) {
            LambdaQueryWrapper<Question> fallbackWrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getSubjectId, subjectId)
                .last("LIMIT 20");
            List<Question> fallback = questionMapper.selectList(fallbackWrapper);
            Collections.shuffle(fallback, random);
            int fallbackLimit = Math.min(baseCount + advancedCount, fallback.size());
            for (int i = 0; i < fallbackLimit; i++) {
                selectedQuestions.add(fallback.get(i));
            }
        }

        return selectedQuestions.stream().map(q -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", q.getId());
            map.put("questionType", q.getQuestionType());
            map.put("questionContent", q.getQuestionContent());
            map.put("questionText", RichContentUtil.toPlainText(q.getQuestionContent()));
            map.put("questionSpeechText", RichContentUtil.toSpeechText(q.getQuestionContent()));
            map.put("questionAudioUrl", RichContentUtil.toSpeechAudioUrl(q.getQuestionContent()));
            map.put("score", q.getScore());
            map.put("timeLimit", q.getTimeLimit());
            map.put("analysis", q.getAnalysis());
            map.put("analysisText", RichContentUtil.toPlainText(q.getAnalysis()));
            map.put("analysisSpeechText", RichContentUtil.toSpeechText(q.getAnalysis()));
            map.put("analysisAudioUrl", RichContentUtil.toSpeechAudioUrl(q.getAnalysis()));

            List<QuestionOption> options = questionOptionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>()
                    .eq(QuestionOption::getQuestionId, q.getId())
                    .orderByAsc(QuestionOption::getSortOrder)
            );
            map.put("options", QuestionRandomizer.toRandomizedOptions(options, random));
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> submitAnswer(Long userId, SubmitAnswerDTO dto) {
        Question question = questionMapper.selectById(dto.getQuestionId());
        if (question == null) {
            throw new BusinessException("题目不存在");
        }

        List<QuestionOption> answerOptions = questionOptionMapper.selectList(
            new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, dto.getQuestionId())
                .orderByAsc(QuestionOption::getSortOrder)
        );
        QuestionAnswerEvaluator.Evaluation evaluation = QuestionAnswerEvaluator.evaluate(question, answerOptions, dto.getAnswer());
        String correctAnswer = evaluation.correctAnswer();
        boolean isCorrect = evaluation.correct();

        Map<String, Object> result = new HashMap<>();
        result.put("correct", isCorrect);
        result.put("correctAnswer", correctAnswer);
        result.put("explanation", question.getAnalysis());
        result.put("explanationText", RichContentUtil.toPlainText(question.getAnalysis()));

        if (isCorrect) {
            result.put("gold", 5);
            result.put("exp", 5);
            result.put("petExp", 2);
            petService.addPetExp(userId, 2);

            WrongTopic existingWrong = wrongTopicMapper.selectOne(
                new LambdaQueryWrapper<WrongTopic>()
                    .eq(WrongTopic::getUserId, userId)
                    .eq(WrongTopic::getQuestionId, dto.getQuestionId())
                    .eq(WrongTopic::getIsMastered, 0)
            );
            if (existingWrong != null) {
                existingWrong.setIsMastered(1);
                wrongTopicMapper.updateById(existingWrong);
            }
        } else {
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

        int totalPossibleScore = level.getTotalQuestions() * 10;
        int correctRate = totalPossibleScore > 0 ? (totalScore * 100 / totalPossibleScore) : 0;

        int stars = calculateStars(correctRate, level.getStarThresholds());
        boolean isPass = correctRate >= level.getPassScore();

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

        LevelResultVO vo = new LevelResultVO();
        vo.setScore(totalScore);
        vo.setCorrectRate(correctRate);
        vo.setStars(stars);
        vo.setWrongCount(wrongCount);
        vo.setIsPass(isPass);

        if (isPass) {
            int goldReward = level.getGoldReward() + (stars == 3 ? 10 : 0);
            int expReward = level.getExpReward();
            vo.setGold(goldReward);
            vo.setExp(expReward);

            User user = userMapper.selectById(userId);
            user.setGold(user.getGold() + goldReward);
            user.setTotalExp(user.getTotalExp() + expReward);
            user.setLevel(calculateLevel(user.getTotalExp()));
            userMapper.updateById(user);
            realtimeEventPublisher.publishBalance(userId, user.getGold(), user.getDiamond());

            RewardLog goldLog = new RewardLog();
            goldLog.setUserId(userId);
            goldLog.setRewardType(1);
            goldLog.setQuantity(goldReward);
            goldLog.setSourceType("COMPLETE_LEVEL");
            goldLog.setSourceId(levelId);
            goldLog.setDescription("完成关卡: " + level.getLevelName());
            rewardLogMapper.insert(goldLog);

            RewardLog expLog = new RewardLog();
            expLog.setUserId(userId);
            expLog.setRewardType(2);
            expLog.setQuantity(expReward);
            expLog.setSourceType("COMPLETE_LEVEL");
            expLog.setSourceId(levelId);
            rewardLogMapper.insert(expLog);

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

            boolean unlockedNext = unlockNextLevel(level, userId, stars);
            vo.setUnlockedNextLevel(unlockedNext);

            updateDailyStats(userId, totalTime, 1, goldReward, expReward);
            achievementService.syncAchievementProgress(userId);

            int petBonusExp = stars * 5;
            petService.addPetExp(userId, petBonusExp);
        } else {
            vo.setGold(0);
            vo.setExp(0);
            vo.setUnlockedNextLevel(false);
        }

        publishChildActivity(userId, level, record, correctRate);
        return vo;
    }

    private void publishChildActivity(Long userId, CourseLevel level, LearningRecord record, int correctRate) {
        FamilyChild familyChild = familyChildMapper.selectOne(
            new LambdaQueryWrapper<FamilyChild>()
                .eq(FamilyChild::getChildUserId, userId)
                .last("LIMIT 1")
        );
        if (familyChild == null) {
            return;
        }
        Family family = familyMapper.selectById(familyChild.getFamilyId());
        if (family == null || family.getParentUserId() == null) {
            return;
        }

        User child = userMapper.selectById(userId);
        List<LearningRecord> todayRecords = learningRecordMapper.selectList(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .ge(LearningRecord::getPlayTime, LocalDate.now().atStartOfDay())
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("childId", userId);
        payload.put("nickname", child != null && child.getNickname() != null ? child.getNickname() : "孩子");
        payload.put("avatar", child != null && child.getAvatar() != null ? child.getAvatar() : "");
        payload.put("online", true);
        payload.put("status", "LEARNING");
        payload.put("todayMinutes", sumTodayLearningMinutes(todayRecords));
        payload.put("completedLevels", (int) todayRecords.stream().filter(r -> Integer.valueOf(1).equals(r.getIsPass())).count());
        payload.put("totalQuestions", sumTodayTotalQuestions(todayRecords));
        payload.put("correctCount", sumTodayCorrectQuestions(todayRecords));
        payload.put("accuracy", correctRate);
        payload.put("currentSubjectName", level != null && level.getSubjectId() != null ? getSubjectName(level.getSubjectId()) : "");
        payload.put("currentLevelName", level != null ? level.getLevelName() : "");
        payload.put("latestScore", record.getScore());
        payload.put("stars", record.getStars());
        payload.put("isPass", Integer.valueOf(1).equals(record.getIsPass()));
        payload.put("lastActivityAt", record.getPlayTime() != null ? record.getPlayTime().toString() : LocalDateTime.now().toString());
        realtimeEventPublisher.publishChildActivity(family.getParentUserId(), payload);
    }

    private String getSubjectName(Long subjectId) {
        Subject subject = subjectMapper.selectById(subjectId);
        return subject != null ? subject.getSubjectName() : "";
    }

    private int sumTodayLearningMinutes(List<LearningRecord> records) {
        return records.stream()
            .mapToInt(record -> Math.max(1, (record.getAnswerTime() == null ? 0 : record.getAnswerTime()) / 60))
            .sum();
    }

    private int sumTodayTotalQuestions(List<LearningRecord> records) {
        int total = 0;
        for (LearningRecord learningRecord : records) {
            CourseLevel recordLevel = courseLevelMapper.selectById(learningRecord.getCourseLevelId());
            total += recordLevel != null && recordLevel.getTotalQuestions() != null ? recordLevel.getTotalQuestions() : 0;
        }
        return total;
    }

    private int sumTodayCorrectQuestions(List<LearningRecord> records) {
        int correct = 0;
        for (LearningRecord learningRecord : records) {
            CourseLevel recordLevel = courseLevelMapper.selectById(learningRecord.getCourseLevelId());
            int questionCount = recordLevel != null && recordLevel.getTotalQuestions() != null ? recordLevel.getTotalQuestions() : 0;
            correct += Math.max(0, questionCount - (learningRecord.getWrongCount() == null ? 0 : learningRecord.getWrongCount()));
        }
        return correct;
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
        return 10 + (totalExp - 5500) / 1200 + 1;
    }

    private boolean unlockNextLevel(CourseLevel currentLevel, Long userId, int earnedStars) {
        CourseLevel nextLevel = courseLevelMapper.selectOne(
            new LambdaQueryWrapper<CourseLevel>()
                .eq(CourseLevel::getSubjectId, currentLevel.getSubjectId())
                .eq(CourseLevel::getLevelNum, currentLevel.getLevelNum() + 1)
                .last("LIMIT 1")
        );
        if (nextLevel == null) {
            return false;
        }

        if (nextLevel.getIsUnlock() == 1) {
            return false;
        }

        String unlockCondition = nextLevel.getUnlockCondition();
        if (unlockCondition != null && unlockCondition.contains("minStars")) {
            try {
                int minStars = extractMinStars(unlockCondition);
                if (earnedStars < minStars) {
                    return false;
                }
            } catch (Exception e) {
                // Keep compatibility with legacy unlockCondition data.
            }
        }

        nextLevel.setIsUnlock(1);
        courseLevelMapper.updateById(nextLevel);
        return true;
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
            existing.setContinuousCorrectCount(0);
            existing.setMasteryLevel(0);
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
            wt.setContinuousCorrectCount(0);
            wt.setMasteryLevel(0);
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
            CourseLevel level = courseLevelMapper.selectById(r.getCourseLevelId());
            if (level != null) {
                map.put("levelName", level.getLevelName());
                if (level.getSubjectId() != null) {
                    Subject subject = subjectMapper.selectById(level.getSubjectId());
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
            map.put("masteryLevel", wt.getMasteryLevel());
            map.put("continuousCorrectCount", wt.getContinuousCorrectCount());
            Question q = questionMapper.selectById(wt.getQuestionId());
            if (q != null) {
                map.put("questionContent", q.getQuestionContent());
                map.put("questionText", RichContentUtil.toPlainText(q.getQuestionContent()));
                map.put("analysisText", RichContentUtil.toPlainText(q.getAnalysis()));
                if (q.getSubjectId() != null) {
                    Subject subject = subjectMapper.selectById(q.getSubjectId());
                    if (subject != null) map.put("subjectName", subject.getSubjectName());
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

    private static final int[] CHECKIN_GOLD  = {5, 10, 15, 20, 30, 40, 50};
    private static final int[] CHECKIN_EXP   = {5, 5, 10, 10, 15, 20, 50};

    @Override
    @Transactional
    public Map<String, Object> checkin(Long userId) {
        LocalDate today = LocalDate.now();
        DailyCheckin existing = dailyCheckinMapper.selectOne(
            new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .eq(DailyCheckin::getCheckinDate, today));
        if (existing != null) {
            throw new BusinessException("今日已签到");
        }
        DailyCheckin yesterday = dailyCheckinMapper.selectOne(
            new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .eq(DailyCheckin::getCheckinDate, today.minusDays(1)));
        int rewardDay = (yesterday != null) ? (yesterday.getRewardDay() % 7) + 1 : 1;
        int gold = CHECKIN_GOLD[rewardDay - 1];
        int exp = CHECKIN_EXP[rewardDay - 1];

        DailyCheckin record = new DailyCheckin();
        record.setUserId(userId);
        record.setCheckinDate(today);
        record.setRewardDay(rewardDay);
        record.setGoldReward(gold);
        record.setExpReward(exp);
        dailyCheckinMapper.insert(record);

        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setGold(user.getGold() + gold);
            user.setTotalExp(user.getTotalExp() + exp);
            userMapper.updateById(user);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("rewardDay", rewardDay);
        result.put("goldReward", gold);
        result.put("expReward", exp);
        result.put("checkedIn", true);
        return result;
    }

    @Override
    public Map<String, Object> getCheckinStatus(Long userId) {
        LocalDate today = LocalDate.now();
        DailyCheckin todayRecord = dailyCheckinMapper.selectOne(
            new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .eq(DailyCheckin::getCheckinDate, today));
        List<DailyCheckin> recentList = dailyCheckinMapper.selectList(
            new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .ge(DailyCheckin::getCheckinDate, today.minusDays(6))
                .orderByAsc(DailyCheckin::getCheckinDate));
        int streak = 0;
        if (todayRecord != null) streak = 1;
        LocalDate d = today.minusDays(1);
        while (true) {
            LocalDate checkDate = d;
            boolean found = recentList.stream().anyMatch(r -> r.getCheckinDate().equals(checkDate));
            if (found) { streak++; d = d.minusDays(1); } else break;
        }
        int nextRewardDay;
        if (todayRecord != null) {
            nextRewardDay = todayRecord.getRewardDay();
        } else if (streak > 0) {
            nextRewardDay = (streak % 7) + 1;
        } else {
            nextRewardDay = 1;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("checkedIn", todayRecord != null);
        result.put("streak", streak);
        result.put("nextRewardDay", nextRewardDay);
        result.put("nextGoldReward", CHECKIN_GOLD[nextRewardDay - 1]);
        result.put("nextExpReward", CHECKIN_EXP[nextRewardDay - 1]);
        result.put("today", today.toString());
        List<Map<String, Object>> weekDays = new ArrayList<>();
        int cycleStart = todayRecord != null ? todayRecord.getRewardDay() : nextRewardDay;
        for (int i = 1; i <= 7; i++) {
            LocalDate date = today.minusDays(cycleStart - i);
            boolean done = recentList.stream().anyMatch(r -> r.getCheckinDate().equals(date));
            Map<String, Object> day = new HashMap<>();
            day.put("day", i);
            day.put("gold", CHECKIN_GOLD[i - 1]);
            day.put("exp", CHECKIN_EXP[i - 1]);
            day.put("done", done);
            day.put("isToday", date.equals(today));
            weekDays.add(day);
        }
        result.put("weekDays", weekDays);
        return result;
    }

    @Override
    public Map<String, Object> getHint(Long userId, Long questionId) {
        List<QuestionOption> options = questionOptionMapper.selectList(
            new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, questionId)
                .orderByAsc(QuestionOption::getSortOrder)
        );
        if (options.size() < 3) {
            throw new BusinessException("题目选项不足，无法使用提示");
        }

        QuestionOption correctOpt = options.stream()
            .filter(o -> o.getIsCorrect() == 1).findFirst().orElse(null);
        if (correctOpt == null) {
            throw new BusinessException("题目没有正确答案");
        }

        List<QuestionOption> wrongOpts = options.stream()
            .filter(o -> o.getIsCorrect() != 1).collect(Collectors.toList());
        Random random = new Random();
        QuestionOption keepWrong = wrongOpts.get(random.nextInt(wrongOpts.size()));

        List<String> keepLabels = List.of(correctOpt.getOptionLabel(), keepWrong.getOptionLabel());
        Map<String, Object> result = new HashMap<>();
        result.put("keepOptions", keepLabels);
        result.put("message", "宠物帮你排除了2个错误选项！");
        return result;
    }

    @Override
    public List<Map<String, Object>> getWeakPoints(Long userId) {
        List<WrongTopic> wrongTopics = wrongTopicMapper.selectList(
            new LambdaQueryWrapper<WrongTopic>()
                .eq(WrongTopic::getUserId, userId)
                .eq(WrongTopic::getIsMastered, 0)
        );

        Map<Long, Long> questionToSubject = new HashMap<>();
        for (WrongTopic wt : wrongTopics) {
            if (!questionToSubject.containsKey(wt.getQuestionId())) {
                Question q = questionMapper.selectById(wt.getQuestionId());
                if (q != null) {
                    questionToSubject.put(wt.getQuestionId(), q.getSubjectId());
                }
            }
        }

        Map<Long, Long> wrongCountBySubject = new HashMap<>();
        for (WrongTopic wt : wrongTopics) {
            Long subjectId = questionToSubject.get(wt.getQuestionId());
            if (subjectId != null) {
                wrongCountBySubject.merge(subjectId, 1L, Long::sum);
            }
        }

        List<LearningRecord> recentRecords = learningRecordMapper.selectList(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .orderByDesc(LearningRecord::getCreateTime)
                .last("LIMIT 20")
        );

        Map<Long, List<LearningRecord>> recordsBySubject = new HashMap<>();
        for (LearningRecord r : recentRecords) {
            CourseLevel level = courseLevelMapper.selectById(r.getCourseLevelId());
            if (level != null && level.getSubjectId() != null) {
                recordsBySubject.computeIfAbsent(level.getSubjectId(), k -> new ArrayList<>()).add(r);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        List<Subject> allSubjects = subjectMapper.selectList(
            new LambdaQueryWrapper<Subject>().eq(Subject::getStatus, 1)
        );

        for (Subject subject : allSubjects) {
            long wrongCount = wrongCountBySubject.getOrDefault(subject.getId(), 0L);
            List<LearningRecord> subjectRecords = recordsBySubject.getOrDefault(subject.getId(), List.of());

            if (wrongCount == 0 && subjectRecords.isEmpty()) continue;

            int totalQuestions = 0;
            int totalCorrect = 0;
            for (LearningRecord r : subjectRecords) {
                CourseLevel level = courseLevelMapper.selectById(r.getCourseLevelId());
                if (level != null) {
                    totalQuestions += level.getTotalQuestions();
                    totalCorrect += level.getTotalQuestions() - r.getWrongCount();
                }
            }
            int accuracy = totalQuestions > 0 ? (totalCorrect * 100 / totalQuestions) : 100;

            if (wrongCount == 0 && accuracy >= 80) continue;

            Map<String, Object> map = new HashMap<>();
            map.put("subjectId", subject.getId());
            map.put("subjectName", subject.getSubjectName());
            map.put("subjectIcon", subject.getIconUrl());
            map.put("wrongCount", wrongCount);
            map.put("accuracy", accuracy);

            result.add(map);
        }

        result.sort((a, b) -> {
            int scoreA = ((Number) a.getOrDefault("wrongCount", 0)).intValue() * 10
                - ((Number) a.getOrDefault("accuracy", 100)).intValue() / 10;
            int scoreB = ((Number) b.getOrDefault("wrongCount", 0)).intValue() * 10
                - ((Number) b.getOrDefault("accuracy", 100)).intValue() / 10;
            return scoreB - scoreA;
        });

        return result.stream().limit(3).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getAdaptiveQuestions(Long userId, Long subjectId) {
        List<Question> selectedQuestions = new ArrayList<>();
        Random random = new Random();

        List<WrongTopic> wrongTopics = wrongTopicMapper.selectList(
            new LambdaQueryWrapper<WrongTopic>()
                .eq(WrongTopic::getUserId, userId)
                .eq(WrongTopic::getIsMastered, 0)
        );

        List<Long> wrongQuestionIds = new ArrayList<>();
        for (WrongTopic wt : wrongTopics) {
            if (subjectId != null) {
                Question q = questionMapper.selectById(wt.getQuestionId());
                if (q == null || !q.getSubjectId().equals(subjectId)) continue;
            }
            wrongQuestionIds.add(wt.getQuestionId());
        }

        Collections.shuffle(wrongQuestionIds, random);
        int wrongCount = Math.min(3, wrongQuestionIds.size());
        for (int i = 0; i < wrongCount; i++) {
            Question q = questionMapper.selectById(wrongQuestionIds.get(i));
            if (q != null) selectedQuestions.add(q);
        }

        int needMore = 5 - selectedQuestions.size();
        if (needMore > 0) {
            Set<Long> existingIds = selectedQuestions.stream().map(Question::getId).collect(Collectors.toSet());
            wrongQuestionIds.forEach(existingIds::add);

            LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .notIn(!existingIds.isEmpty(), Question::getId, existingIds);

            if (subjectId != null) {
                wrapper.eq(Question::getSubjectId, subjectId);
            }

            wrapper.last("LIMIT " + needMore * 3);
            List<Question> candidates = questionMapper.selectList(wrapper);
            Collections.shuffle(candidates, random);
            for (int i = 0; i < Math.min(needMore, candidates.size()); i++) {
                selectedQuestions.add(candidates.get(i));
            }
        }

        return QuestionRandomizer.shuffledCopy(selectedQuestions, random).stream().map(q -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", q.getId());
            map.put("questionType", q.getQuestionType());
            map.put("questionContent", q.getQuestionContent());
            map.put("questionText", RichContentUtil.toPlainText(q.getQuestionContent()));
            map.put("questionSpeechText", RichContentUtil.toSpeechText(q.getQuestionContent()));
            map.put("questionAudioUrl", RichContentUtil.toSpeechAudioUrl(q.getQuestionContent()));
            map.put("score", q.getScore());
            map.put("timeLimit", q.getTimeLimit());
            map.put("analysis", q.getAnalysis());

            List<QuestionOption> options = questionOptionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>()
                    .eq(QuestionOption::getQuestionId, q.getId())
                    .orderByAsc(QuestionOption::getSortOrder)
            );
            map.put("options", QuestionRandomizer.toRandomizedOptions(options, random));
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> explainWrong(Long userId, Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("questionId", questionId);
        result.put("analysisText", RichContentUtil.toPlainText(question.getAnalysis()));

        List<QuestionOption> options = questionOptionMapper.selectList(
            new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, questionId)
                .orderByAsc(QuestionOption::getSortOrder)
        );
        List<String> optionTexts = options.stream()
            .map(o -> RichContentUtil.toPlainText(o.getOptionContent()))
            .collect(Collectors.toList());
        result.put("options", optionTexts);

        WrongTopic wt = wrongTopicMapper.selectOne(
            new LambdaQueryWrapper<WrongTopic>()
                .eq(WrongTopic::getUserId, userId)
                .eq(WrongTopic::getQuestionId, questionId)
                .last("LIMIT 1")
        );
        String wrongAnswer = wt != null ? wt.getWrongAnswer() : "";
        String correctAnswer = wt != null ? wt.getCorrectAnswer() : "";

        String aiAvailable = "false";
        String aiExplanation = null;
        if (aiService.isAvailable()) {
            String questionText = RichContentUtil.toPlainText(question.getQuestionContent());
            aiExplanation = aiService.explainWrongTopic(questionText, wrongAnswer, correctAnswer, optionTexts);
            if (aiExplanation != null) {
                aiAvailable = "true";
            }
        }
        result.put("aiAvailable", aiAvailable);
        result.put("aiExplanation", aiExplanation);
        result.put("wrongAnswer", wrongAnswer);
        result.put("correctAnswer", correctAnswer);

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> retryWrong(Long userId, Long questionId, String answer) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }

        QuestionOption correctOption = questionOptionMapper.selectOne(
            new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, questionId)
                .eq(QuestionOption::getIsCorrect, 1)
                .last("LIMIT 1")
        );

        String correctAnswer = correctOption != null ? correctOption.getOptionLabel() : "";
        boolean isCorrect = correctAnswer.equalsIgnoreCase(answer);

        Map<String, Object> result = new HashMap<>();
        result.put("correct", isCorrect);
        result.put("correctAnswer", correctAnswer);
        result.put("explanation", question.getAnalysis());
        result.put("explanationText", RichContentUtil.toPlainText(question.getAnalysis()));

        if (isCorrect) {
            WrongTopic wt = wrongTopicMapper.selectOne(
                new LambdaQueryWrapper<WrongTopic>()
                    .eq(WrongTopic::getUserId, userId)
                    .eq(WrongTopic::getQuestionId, questionId)
            );
            if (wt != null) {
                wt.setIsMastered(1);
                wrongTopicMapper.updateById(wt);
            }
            result.put("mastered", true);
            result.put("gold", 3);
            result.put("exp", 3);
            petService.addPetExp(userId, 1);
        } else {
            saveWrongTopic(userId, questionId, answer, correctAnswer);
            result.put("mastered", false);
            result.put("gold", 0);
            result.put("exp", 0);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getAssessmentQuestions(Long userId) {
        User user = userMapper.selectById(userId);
        ChildProfile profile = childProfileMapper.selectOne(new LambdaQueryWrapper<ChildProfile>().eq(ChildProfile::getUserId, userId));
        Long gradeLevelId = profile != null && profile.getGradeLevel() != null ? profile.getGradeLevel().longValue() : null;

        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (gradeLevelId != null) {
            wrapper.eq(Question::getGradeLevelId, gradeLevelId);
        }
        wrapper.orderByAsc(Question::getId).last("LIMIT 50");

        List<Question> pool = questionMapper.selectList(wrapper);
        Random random = new Random();
        Collections.shuffle(pool, random);

        List<Question> selected = pool.subList(0, Math.min(10, pool.size()));

        return selected.stream().map(q -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", q.getId());
            map.put("questionType", q.getQuestionType());
            map.put("questionContent", q.getQuestionContent());
            map.put("questionText", RichContentUtil.toPlainText(q.getQuestionContent()));
            map.put("questionSpeechText", RichContentUtil.toSpeechText(q.getQuestionContent()));
            map.put("questionAudioUrl", RichContentUtil.toSpeechAudioUrl(q.getQuestionContent()));
            map.put("score", q.getScore());
            map.put("timeLimit", q.getTimeLimit());

            List<QuestionOption> options = questionOptionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>()
                    .eq(QuestionOption::getQuestionId, q.getId())
                    .orderByAsc(QuestionOption::getSortOrder)
            );
            map.put("options", QuestionRandomizer.toRandomizedOptions(options, random));
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<PracticeModeVO> getPracticeModes(Long userId, Long subjectId) {
        List<PracticeModeVO> modes = new ArrayList<>();
        PracticeModeVO vo = new PracticeModeVO();
        vo.setId(subjectId != null ? subjectId : 1L);
        vo.setName("综合练习");
        vo.setDescription("涵盖所选学科所有知识点的大量题目练习");
        vo.setIcon("📚");
        vo.setType("ENDLESS");
        vo.setTimeLimitSeconds(null);
        modes.add(vo);
        return modes;
    }

    @Override
    public Map<String, Object> startPractice(Long userId, Long practiceModeId) {
        Long subjectId = practiceModeId;

        User user = userMapper.selectById(userId);
        ChildProfile profile = childProfileMapper.selectOne(new LambdaQueryWrapper<ChildProfile>().eq(ChildProfile::getUserId, userId));
        Long gradeLevelId = profile != null && profile.getGradeLevel() != null ? profile.getGradeLevel().longValue() : null;

        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>();
        wrapper.eq(Question::getSubjectId, subjectId);
        if (gradeLevelId != null) {
            wrapper.eq(Question::getGradeLevelId, gradeLevelId);
        }

        List<Question> questions = questionMapper.selectList(wrapper);
        if (questions.isEmpty()) {
            throw new BusinessException("该学科年级下暂无题目");
        }

        Collections.shuffle(questions);

        List<Map<String, Object>> formattedQuestions = questions.stream()
            .map(this::formatQuestion)
            .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("practiceSessionId", System.currentTimeMillis());
        result.put("modeId", subjectId);
        result.put("modeName", "专项练习");
        result.put("type", "ENDLESS");
        result.put("timeLimit", 0);
        result.put("questions", formattedQuestions);
        return result;
    }

    private Map<String, Object> formatQuestion(Question q) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", q.getId());
        map.put("questionType", q.getQuestionType());
        map.put("questionContent", q.getQuestionContent());
        map.put("questionText", RichContentUtil.toPlainText(q.getQuestionContent()));

        List<QuestionOption> options = questionOptionMapper.selectList(
            new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, q.getId())
                .orderByAsc(QuestionOption::getSortOrder)
        );
        map.put("options", QuestionRandomizer.toRandomizedOptions(options, new Random()));
        return map;
    }

    @Override
    public Map<String, Object> submitPracticeAnswer(Long userId, Long practiceSessionId, SubmitAnswerDTO dto) {
        return submitAnswer(userId, dto);
    }

    @Override
    public SmartReviewQuizVO getSmartReviewQuiz(Long userId, Long subjectId, Integer questionCount) {
        LambdaQueryWrapper<WrongTopic> wrapper = new LambdaQueryWrapper<WrongTopic>()
            .eq(WrongTopic::getUserId, userId)
            .eq(WrongTopic::getIsMastered, 0)
            .orderByAsc(WrongTopic::getContinuousCorrectCount)
            .orderByAsc(WrongTopic::getLastReviewTime)
            .last("LIMIT " + questionCount);

        List<WrongTopic> topics = wrongTopicMapper.selectList(wrapper);

        SmartReviewQuizVO vo = new SmartReviewQuizVO();
        vo.setTotalQuestions(topics.size());
        vo.setEstimatedMinutes((topics.size() * 2));
        vo.setQuestionIds(topics.stream().map(WrongTopic::getQuestionId).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public Map<String, Object> updateWrongTopicMastery(Long userId, Long wrongTopicId, boolean isCorrect) {
        WrongTopic wt = wrongTopicMapper.selectById(wrongTopicId);
        if (wt == null || !wt.getUserId().equals(userId)) {
            throw new BusinessException("错题记录不存在");
        }

        wt.setLastReviewTime(LocalDateTime.now());
        if (isCorrect) {
            wt.setContinuousCorrectCount((wt.getContinuousCorrectCount() == null ? 0 : wt.getContinuousCorrectCount()) + 1);
            if (wt.getContinuousCorrectCount() >= 3) {
                wt.setIsMastered(1);
                wt.setMasteryLevel(2);
            } else {
                wt.setMasteryLevel(1);
            }
        } else {
            wt.setContinuousCorrectCount(0);
            wt.setMasteryLevel(0);
            wt.setTimes(wt.getTimes() + 1);
            wt.setLastWrongTime(LocalDateTime.now());
        }

        wrongTopicMapper.updateById(wt);

        Map<String, Object> res = new HashMap<>();
        res.put("mastered", wt.getIsMastered() == 1);
        res.put("continuousCorrectCount", wt.getContinuousCorrectCount());
        res.put("masteryLevel", wt.getMasteryLevel());
        return res;
    }
}
