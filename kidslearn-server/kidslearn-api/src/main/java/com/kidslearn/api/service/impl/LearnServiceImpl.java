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
import com.kidslearn.api.service.EntitlementService;
import com.kidslearn.common.exception.BusinessException;
import com.kidslearn.common.util.RichContentUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
    private final EntitlementService entitlementService;
    private final LearningAccessService learningAccessService;
    private final PracticeSessionMapper practiceSessionMapper;
    private final UserQuestionRecordMapper userQuestionRecordMapper;

    @Override
    public Map<String, Object> getAccessStatus(Long userId) {
        return learningAccessService.getAccessStatus(userId);
    }

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

        boolean premiumAllowed = entitlementService.has(userId, EntitlementService.Code.COURSE_PREMIUM);

        List<Long> videoIds = videos.stream().map(CourseVideo::getId).collect(Collectors.toList());
        Map<Long, UserVideoProgress> progressByVideoId = userVideoProgressMapper.selectList(
            new LambdaQueryWrapper<UserVideoProgress>()
                .eq(UserVideoProgress::getUserId, userId)
                .in(UserVideoProgress::getVideoId, videoIds)
        ).stream().collect(Collectors.toMap(UserVideoProgress::getVideoId, p -> p, (left, right) -> left));

        return videos.stream()
            .map(video -> {
                Map<String, Object> map = toVideoMap(video, progressByVideoId.get(video.getId()));
                // 非VIP用户标记付费视频为锁定状态
                if (!premiumAllowed && (video.getIsFree() == null || video.getIsFree() != 1)) {
                    map.put("locked", true);
                    map.remove("videoUrl");
                }
                return map;
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> submitVideoProgress(Long userId, SubmitVideoProgressDTO dto) {
        learningAccessService.checkAccess(userId, LearningAccessService.Scene.VIDEO);
        if (dto == null || dto.getVideoId() == null) {
            throw new BusinessException("Video id is required");
        }
        CourseVideo video = courseVideoMapper.selectById(dto.getVideoId());
        if (video == null || video.getStatus() == null || video.getStatus() != 1) {
            throw new BusinessException("Video not found");
        }
        if (video.getIsFree() == null || video.getIsFree() != 1) {
            entitlementService.require(userId, EntitlementService.Code.COURSE_PREMIUM);
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
        learningAccessService.checkAccess(userId, LearningAccessService.Scene.LEVEL_QUIZ);

        CourseLevel level = courseLevelMapper.selectById(levelId);
        if (level == null || level.getSubjectId() == null) {
            return List.of();
        }

        Long subjectId = level.getSubjectId();
        int levelNum = level.getLevelNum() != null ? level.getLevelNum() : 1;
        int baseCount = level.getBaseQuestionCount() != null ? level.getBaseQuestionCount() : 12;
        int advancedCount = level.getAdvancedQuestionCount() != null ? level.getAdvancedQuestionCount() : 3;
        int totalCount = baseCount + advancedCount;

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

        // 查询用户最近答过的题目ID，避免短期内重复出题
        Set<Long> recentQuestionIds = getRecentAnsweredQuestionIds(userId, subjectId, totalCount);

        Random random = new Random();
        List<Question> selectedQuestions = new ArrayList<>();

        if (userGradeLevelId != null) {
            // 基础题：从当前年级抽取，按关卡号哈希分桶保证不同关卡题目不重叠
            List<Question> baseQuestions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                    .eq(Question::getSubjectId, subjectId)
                    .eq(Question::getGradeLevelId, userGradeLevelId)
            );
            List<Question> basePool = pickByLevelSlot(baseQuestions, levelNum, baseCount, recentQuestionIds, random);
            selectedQuestions.addAll(basePool);

            // 高阶题：从下一年级抽取（同关卡分桶逻辑，但桶号偏移避免与基础题完全重合）
            Long nextGradeLevelId = userGradeLevelId + 1;
            List<Question> advancedQuestions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                    .eq(Question::getSubjectId, subjectId)
                    .eq(Question::getGradeLevelId, nextGradeLevelId)
            );
            List<Question> advPool = pickByLevelSlot(advancedQuestions, levelNum, advancedCount, recentQuestionIds, random);
            selectedQuestions.addAll(advPool);
        }

        // 兜底：题库不足时从同学科任意年级补齐
        if (selectedQuestions.size() < totalCount) {
            int need = totalCount - selectedQuestions.size();
            Set<Long> alreadyIds = selectedQuestions.stream().map(Question::getId).collect(Collectors.toSet());
            List<Question> fallback = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                    .eq(Question::getSubjectId, subjectId)
                    .notIn(!alreadyIds.isEmpty(), Question::getId, alreadyIds)
                    .last("LIMIT " + (need * 3))
            );
            Collections.shuffle(fallback, random);
            for (int i = 0; i < Math.min(need, fallback.size()); i++) {
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
            map.put("options", QuestionRandomizer.toRandomizedOptions(options, random, q.getQuestionType()));
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> submitAnswer(Long userId, SubmitAnswerDTO dto) {
        learningAccessService.checkAccess(userId, LearningAccessService.Scene.LEVEL_QUIZ);
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
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            user.setGold((user.getGold() != null ? user.getGold() : 0) + goldReward);
            user.setTotalExp((user.getTotalExp() != null ? user.getTotalExp() : 0) + expReward);
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
            existing.setTimes((existing.getTimes() == null ? 0 : existing.getTimes()) + 1);
            existing.setWrongAnswer(wrongAnswer);
            existing.setCorrectAnswer(correctAnswer);
            existing.setLastWrongTime(LocalDateTime.now());
            existing.setIsMastered(0);
            existing.setContinuousCorrectCount(0);
            existing.setMasteryLevel(0);
            existing.setNextReviewDate(LocalDate.now().plusDays(1));
            existing.setLastReviewResult(0);
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
            wt.setNextReviewDate(LocalDate.now().plusDays(1));
            wt.setReviewCount(0);
            wt.setLastReviewResult(0);
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
            map.put("nextReviewDate", wt.getNextReviewDate());
            map.put("reviewCount", wt.getReviewCount());
            map.put("due", wt.getNextReviewDate() == null || !wt.getNextReviewDate().isAfter(LocalDate.now()));
            Question q = questionMapper.selectById(wt.getQuestionId());
            if (q != null) {
                map.put("questionContent", q.getQuestionContent());
                map.put("questionText", RichContentUtil.toPlainText(q.getQuestionContent()));
                map.put("analysisText", RichContentUtil.toPlainText(q.getAnalysis()));
                if (q.getSubjectId() != null) {
                    map.put("subjectId", q.getSubjectId());
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
        // Include today in streak count when not yet checked in
        if (todayRecord == null) {
            streak++;
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
        learningAccessService.checkAccess(userId, LearningAccessService.Scene.ADAPTIVE);
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
            map.put("options", QuestionRandomizer.toRandomizedOptions(options, random, q.getQuestionType()));
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
        learningAccessService.checkAccess(userId, LearningAccessService.Scene.WRONG_RETRY);
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

        WrongTopic wt = wrongTopicMapper.selectOne(new LambdaQueryWrapper<WrongTopic>()
            .eq(WrongTopic::getUserId, userId).eq(WrongTopic::getQuestionId, questionId).last("LIMIT 1"));
        if (isCorrect) {
            Map<String, Object> mastery = wt == null ? Map.of("mastered", false) : updateWrongTopicMastery(userId, wt.getId(), true);
            result.put("mastered", mastery.get("mastered"));
            result.put("nextReviewDate", mastery.get("nextReviewDate"));
            result.put("gold", 3);
            result.put("exp", 3);
            petService.addPetExp(userId, 1);
        } else {
            if (wt == null) saveWrongTopic(userId, questionId, answer, correctAnswer);
            else {
                wt.setWrongAnswer(answer);
                wt.setCorrectAnswer(correctAnswer);
                updateWrongTopicMastery(userId, wt.getId(), false);
            }
            result.put("mastered", false);
            result.put("gold", 0);
            result.put("exp", 0);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getAssessmentQuestions(Long userId) {
        learningAccessService.checkAccess(userId, LearningAccessService.Scene.ASSESSMENT);
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
            map.put("options", QuestionRandomizer.toRandomizedOptions(options, random, q.getQuestionType()));
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<PracticeModeVO> getPracticeModes(Long userId, Long subjectId) {
        List<PracticeMode> configuredModes = practiceModeMapper.selectList(
            new LambdaQueryWrapper<PracticeMode>()
                .eq(PracticeMode::getStatus, 1)
                .eq(subjectId != null, PracticeMode::getSubjectId, subjectId)
                .orderByAsc(PracticeMode::getSortOrder)
                .orderByAsc(PracticeMode::getId)
        );
        if (!configuredModes.isEmpty()) {
            return configuredModes.stream().map(m -> toPracticeModeVO(m, userId)).collect(Collectors.toList());
        }
        // 数据库未配置时兜底返回 3 种标准模式（驾考宝典风格）
        Long baseId = subjectId != null ? subjectId * 1000L : 1000L;
        Long userGradeLevelId = resolveGradeLevelId(userId);
        Integer questionCount = countAvailableQuestions(subjectId, userGradeLevelId);
        return List.of(
            buildStandardMode(baseId + 1, subjectId, "SEQUENTIAL", "顺序练习", "按顺序逐题练习，可随时回看，支持断点续做", "📋", 1, questionCount),
            buildStandardMode(baseId + 2, subjectId, "RANDOM", "随机练习", "打乱顺序刷题，巩固薄弱知识点", "🎲", 2, questionCount),
            buildStandardMode(baseId + 3, subjectId, "MOCK_EXAM", "模拟考试", "模拟真实考试，限时作答检验水平", "📝", 3, questionCount)
        );
    }

    @Override
    @Transactional
    public Map<String, Object> startPractice(Long userId, Long practiceModeId) {
        learningAccessService.checkAccess(userId, LearningAccessService.Scene.PRACTICE);
        PracticeMode mode = practiceModeMapper.selectById(practiceModeId);
        // 兜底模式（id 未在 practice_mode 表中）解析出标准模式和学科
        StandardModeInfo stdInfo = mode == null ? parseStandardModeId(practiceModeId) : null;
        if (mode == null && stdInfo == null) {
            throw new BusinessException("练习模式不存在");
        }
        if (mode != null && !Integer.valueOf(1).equals(mode.getStatus())) {
            throw new BusinessException("练习模式已下线");
        }

        Long subjectId = mode != null ? mode.getSubjectId() : stdInfo.subjectId;
        String modeType = mode != null && mode.getType() != null ? mode.getType() : (stdInfo != null ? stdInfo.type : "SEQUENTIAL");
        String modeName = mode != null ? mode.getName() : (stdInfo != null ? stdInfo.name : "专项练习");

        Long gradeLevelId = resolveGradeLevelId(userId);

        // 若该模式下已有 IN_PROGRESS 会话，直接走续做路径（断点续做核心）
        PracticeSession existing = practiceSessionMapper.selectOne(
            new LambdaQueryWrapper<PracticeSession>()
                .eq(PracticeSession::getUserId, userId)
                .eq(PracticeSession::getPracticeModeId, practiceModeId)
                .eq(PracticeSession::getStatus, "IN_PROGRESS")
                .orderByDesc(PracticeSession::getUpdateTime)
                .last("LIMIT 1")
        );
        if (existing != null) {
            return buildResumeResult(existing);
        }

        // 全量选题
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
            .eq(Question::getSubjectId, subjectId);
        if (gradeLevelId != null) {
            wrapper.eq(Question::getGradeLevelId, gradeLevelId);
        }
        if ("SEQUENTIAL".equals(modeType)) {
            // 顺序练习：按 sortOrder 稳定排序，若 sortOrder 全为 null 则按 id 升序（题目录入顺序）
            wrapper.orderByAsc(Question::getSortOrder).orderByAsc(Question::getId);
        }
        List<Question> questions = questionMapper.selectList(wrapper);
        if (questions.isEmpty()) {
            throw new BusinessException("该学科年级下暂无题目");
        }
        if (!"SEQUENTIAL".equals(modeType)) {
            // RANDOM / MOCK_EXAM：打乱顺序
            Collections.shuffle(questions);
        }
        if ("MOCK_EXAM".equals(modeType) && questions.size() > 20) {
            questions = questions.subList(0, 20);
        }

        // 固化题目顺序到 session 快照
        String questionIdsSnapshot = questions.stream().map(q -> String.valueOf(q.getId())).collect(Collectors.joining(","));

        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setPracticeModeId(practiceModeId);
        session.setSubjectId(subjectId);
        session.setGradeLevelId(gradeLevelId);
        session.setQuestionIds(questionIdsSnapshot);
        session.setTotalQuestions(questions.size());
        session.setCurrentIndex(0);
        session.setCorrectCount(0);
        session.setWrongCount(0);
        session.setStatus("IN_PROGRESS");
        session.setLastActiveTime(java.time.LocalDateTime.now());
        practiceSessionMapper.insert(session);

        List<Map<String, Object>> formattedQuestions = questions.stream().map(this::formatQuestion).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("practiceSessionId", session.getId()); // 兼容旧字段
        result.put("modeId", practiceModeId);
        result.put("modeName", modeName);
        result.put("type", modeType);
        result.put("timeLimit", mode != null && mode.getTimeLimitSeconds() != null ? mode.getTimeLimitSeconds() : 0);
        result.put("currentIndex", 0);
        result.put("totalQuestions", questions.size());
        result.put("questions", formattedQuestions);
        result.put("userRecords", new HashMap<>()); // 新会话无历史记录
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> submitPracticeAnswer(Long userId, Long practiceSessionId, SubmitAnswerDTO dto) {
        learningAccessService.checkAccess(userId, LearningAccessService.Scene.PRACTICE);
        log.info("专项练习答题 - userId: {}, sessionId: {}, questionId: {}", userId, practiceSessionId, dto.getQuestionId());

        PracticeSession session = practiceSessionMapper.selectById(practiceSessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException("练习会话不存在");
        }

        // 复用 submitAnswer 判对错（保留原奖励、错题入库逻辑）
        Map<String, Object> result = submitAnswer(userId, dto);
        boolean isCorrect = Boolean.TRUE.equals(result.get("correct"));

        // 计算该题在 session 中的位置（基于固化顺序）
        int questionIndex = -1;
        if (session.getQuestionIds() != null) {
            String[] ids = session.getQuestionIds().split(",");
            for (int i = 0; i < ids.length; i++) {
                if (ids[i].equals(String.valueOf(dto.getQuestionId()))) {
                    questionIndex = i;
                    break;
                }
            }
        }

        // 写入 user_question_record（upsert：同一会话同一题只保留最新一次）
        UserQuestionRecord existingRecord = userQuestionRecordMapper.selectOne(
            new LambdaQueryWrapper<UserQuestionRecord>()
                .eq(UserQuestionRecord::getSessionId, practiceSessionId)
                .eq(UserQuestionRecord::getQuestionId, dto.getQuestionId())
                .last("LIMIT 1")
        );
        if (existingRecord == null) {
            UserQuestionRecord record = new UserQuestionRecord();
            record.setUserId(userId);
            record.setSessionId(practiceSessionId);
            record.setQuestionId(dto.getQuestionId());
            record.setPracticeModeId(session.getPracticeModeId());
            record.setSource(session.getStatus());
            record.setUserAnswer(dto.getAnswer());
            record.setIsCorrect(isCorrect ? 1 : 0);
            record.setAnswerTimeMs(dto.getAnswerTime() != null ? dto.getAnswerTime() * 1000 : null);
            userQuestionRecordMapper.insert(record);
            // 只有首次作答才累加计数，避免重复提交导致统计膨胀
            session.setCorrectCount(session.getCorrectCount() + (isCorrect ? 1 : 0));
            session.setWrongCount(session.getWrongCount() + (isCorrect ? 0 : 1));
        } else {
            // 更新已有记录（重新作答）
            boolean wasCorrect = Integer.valueOf(1).equals(existingRecord.getIsCorrect());
            existingRecord.setUserAnswer(dto.getAnswer());
            existingRecord.setIsCorrect(isCorrect ? 1 : 0);
            existingRecord.setAnswerTimeMs(dto.getAnswerTime() != null ? dto.getAnswerTime() * 1000 : null);
            userQuestionRecordMapper.updateById(existingRecord);
            // 同步修正统计（仅当正确性状态变化时调整）
            if (wasCorrect && !isCorrect) {
                session.setCorrectCount(Math.max(0, session.getCorrectCount() - 1));
                session.setWrongCount(session.getWrongCount() + 1);
            } else if (!wasCorrect && isCorrect) {
                session.setWrongCount(Math.max(0, session.getWrongCount() - 1));
                session.setCorrectCount(session.getCorrectCount() + 1);
            }
        }

        // 推进当前进度（仅当提交的是当前题或之前的题时，currentIndex 取 max）
        if (questionIndex >= 0) {
            int nextIndex = questionIndex + 1;
            if (nextIndex > session.getCurrentIndex()) {
                session.setCurrentIndex(Math.min(nextIndex, session.getTotalQuestions()));
            }
        }
        session.setLastActiveTime(java.time.LocalDateTime.now());
        practiceSessionMapper.updateById(session);

        result.put("questionIndex", questionIndex);
        result.put("currentIndex", session.getCurrentIndex());
        result.put("correctCount", session.getCorrectCount());
        result.put("wrongCount", session.getWrongCount());
        return result;
    }

    @Override
    public Map<String, Object> resumePractice(Long userId, Long sessionId) {
        learningAccessService.checkAccess(userId, LearningAccessService.Scene.PRACTICE);
        PracticeSession session = practiceSessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException("练习会话不存在");
        }
        return buildResumeResult(session);
    }

    @Override
    public Map<String, Object> getPracticeProgress(Long userId, Long practiceModeId) {
        PracticeSession session = practiceSessionMapper.selectOne(
            new LambdaQueryWrapper<PracticeSession>()
                .eq(PracticeSession::getUserId, userId)
                .eq(PracticeSession::getPracticeModeId, practiceModeId)
                .eq(PracticeSession::getStatus, "IN_PROGRESS")
                .orderByDesc(PracticeSession::getUpdateTime)
                .last("LIMIT 1")
        );
        Map<String, Object> result = new HashMap<>();
        if (session == null) {
            result.put("hasSession", false);
            return result;
        }
        int answered = session.getCorrectCount() + session.getWrongCount();
        result.put("hasSession", true);
        result.put("sessionId", session.getId());
        result.put("currentIndex", session.getCurrentIndex());
        result.put("total", session.getTotalQuestions());
        result.put("answered", answered);
        result.put("correct", session.getCorrectCount());
        result.put("wrong", session.getWrongCount());
        result.put("lastActiveTime", session.getLastActiveTime());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> abandonPractice(Long userId, Long sessionId) {
        PracticeSession session = practiceSessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException("练习会话不存在");
        }
        session.setStatus("ABANDONED");
        practiceSessionMapper.updateById(session);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> completePracticeSession(Long userId, Long sessionId) {
        PracticeSession session = practiceSessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException("练习会话不存在");
        }
        session.setStatus("COMPLETED");
        session.setLastActiveTime(java.time.LocalDateTime.now());
        practiceSessionMapper.updateById(session);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("correct", session.getCorrectCount());
        result.put("wrong", session.getWrongCount());
        result.put("total", session.getTotalQuestions());
        return result;
    }

    /** 组装续做结果（题目顺序、用户答题记录、当前进度） */
    private Map<String, Object> buildResumeResult(PracticeSession session) {
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("practiceSessionId", session.getId());
        result.put("modeId", session.getPracticeModeId());
        result.put("currentIndex", session.getCurrentIndex());
        result.put("totalQuestions", session.getTotalQuestions());
        result.put("correctCount", session.getCorrectCount());
        result.put("wrongCount", session.getWrongCount());

        // 按固化顺序批量加载题目
        List<Map<String, Object>> questions = new ArrayList<>();
        if (session.getQuestionIds() != null && !session.getQuestionIds().isEmpty()) {
            String[] idStrs = session.getQuestionIds().split(",");
            List<Long> ids = new ArrayList<>();
            for (String s : idStrs) {
                if (!s.isEmpty()) ids.add(Long.parseLong(s.trim()));
            }
            if (!ids.isEmpty()) {
                List<Question> qlist = questionMapper.selectBatchIds(ids);
                Map<Long, Question> qmap = qlist.stream().collect(Collectors.toMap(Question::getId, q -> q));
                for (Long id : ids) {
                    Question q = qmap.get(id);
                    if (q != null) questions.add(formatQuestion(q));
                }
            }
        }
        result.put("questions", questions);

        // 加载该会话所有答题记录，组装为 { questionId: { answer, displayAnswer, isCorrect, correctAnswer } }
        List<UserQuestionRecord> records = userQuestionRecordMapper.selectList(
            new LambdaQueryWrapper<UserQuestionRecord>()
                .eq(UserQuestionRecord::getSessionId, session.getId())
        );
        Map<Long, UserQuestionRecord> recordMap = records.stream()
            .collect(Collectors.toMap(UserQuestionRecord::getQuestionId, r -> r, (a, b) -> b));
        result.put("userRecords", recordMap);
        return result;
    }

    private PracticeModeVO toPracticeModeVO(PracticeMode mode, Long userId) {
        PracticeModeVO vo = new PracticeModeVO();
        vo.setId(mode.getId());
        vo.setName(mode.getName());
        vo.setDescription(mode.getDescription());
        vo.setIcon(mode.getIcon());
        vo.setType(mode.getType());
        vo.setTimeLimitSeconds(mode.getTimeLimitSeconds());
        vo.setTags(mode.getTags());
        vo.setSubjectId(mode.getSubjectId());
        vo.setSortOrder(mode.getSortOrder());
        Long userGradeLevelId = resolveGradeLevelId(userId);
        vo.setQuestionCount(countAvailableQuestions(mode.getSubjectId(), userGradeLevelId));
        return vo;
    }

    private PracticeModeVO buildStandardMode(Long id, Long subjectId, String type, String name, String desc, String icon, int sortOrder, Integer questionCount) {
        PracticeModeVO vo = new PracticeModeVO();
        vo.setId(id);
        vo.setSubjectId(subjectId);
        vo.setName(name);
        vo.setDescription(desc);
        vo.setIcon(icon);
        vo.setType(type);
        vo.setSortOrder(sortOrder);
        vo.setQuestionCount(questionCount);
        vo.setTimeLimitSeconds("MOCK_EXAM".equals(type) ? 600 : null);
        return vo;
    }

    /** 兜底模式 ID 解析：subjectId*1000 + N 反推学科与模式类型 */
    private StandardModeInfo parseStandardModeId(Long practiceModeId) {
        if (practiceModeId == null || practiceModeId < 1000) return null;
        long subjectId = practiceModeId / 1000;
        long suffix = practiceModeId % 1000;
        String type;
        String name;
        if (suffix == 1) { type = "SEQUENTIAL"; name = "顺序练习"; }
        else if (suffix == 2) { type = "RANDOM"; name = "随机练习"; }
        else if (suffix == 3) { type = "MOCK_EXAM"; name = "模拟考试"; }
        else return null;
        return new StandardModeInfo(subjectId, type, name);
    }

    private static class StandardModeInfo {
        final Long subjectId;
        final String type;
        final String name;
        StandardModeInfo(Long subjectId, String type, String name) {
            this.subjectId = subjectId; this.type = type; this.name = name;
        }
    }

    private Long resolveGradeLevelId(Long userId) {
        ChildProfile profile = childProfileMapper.selectOne(
            new LambdaQueryWrapper<ChildProfile>().eq(ChildProfile::getUserId, userId));
        return profile != null && profile.getGradeLevel() != null ? profile.getGradeLevel().longValue() : null;
    }

    private Integer countAvailableQuestions(Long subjectId, Long gradeLevelId) {
        if (subjectId == null) return 0;
        LambdaQueryWrapper<Question> w = new LambdaQueryWrapper<Question>().eq(Question::getSubjectId, subjectId);
        if (gradeLevelId != null) w.eq(Question::getGradeLevelId, gradeLevelId);
        return Math.toIntExact(questionMapper.selectCount(w));
    }

    private Map<String, Object> formatQuestion(Question q) {
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
        map.put("options", QuestionRandomizer.toRandomizedOptions(options, new Random(), q.getQuestionType()));
        return map;
    }

    @Override
    public SmartReviewQuizVO getSmartReviewQuiz(Long userId, Long subjectId, Integer questionCount) {
        learningAccessService.checkAccess(userId, LearningAccessService.Scene.WRONG_RETRY);
        LambdaQueryWrapper<WrongTopic> wrapper = new LambdaQueryWrapper<WrongTopic>()
            .eq(WrongTopic::getUserId, userId)
            .eq(WrongTopic::getIsMastered, 0)
            .and(w -> w.isNull(WrongTopic::getNextReviewDate).or().le(WrongTopic::getNextReviewDate, LocalDate.now()))
            .orderByAsc(WrongTopic::getContinuousCorrectCount)
            .orderByAsc(WrongTopic::getNextReviewDate);

        List<WrongTopic> topics = wrongTopicMapper.selectList(wrapper);
        if (subjectId != null) {
            topics = topics.stream().filter(wt -> {
                Question q = questionMapper.selectById(wt.getQuestionId());
                return q != null && subjectId.equals(q.getSubjectId());
            }).toList();
        }
        int safeCount = Math.max(1, Math.min(questionCount == null ? 15 : questionCount, 50));
        topics = topics.stream().limit(safeCount).toList();

        SmartReviewQuizVO vo = new SmartReviewQuizVO();
        vo.setTotalQuestions(topics.size());
        vo.setEstimatedMinutes((topics.size() * 2));
        vo.setQuestionIds(topics.stream().map(WrongTopic::getQuestionId).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public List<Map<String, Object>> getDueReviewQuestions(Long userId, Long subjectId, Integer questionCount) {
        learningAccessService.checkAccess(userId, LearningAccessService.Scene.WRONG_RETRY);
        int limit = Math.max(1, Math.min(questionCount == null ? 15 : questionCount, 50));
        return wrongTopicMapper.selectList(new LambdaQueryWrapper<WrongTopic>()
                .eq(WrongTopic::getUserId, userId).eq(WrongTopic::getIsMastered, 0)
                .and(w -> w.isNull(WrongTopic::getNextReviewDate).or().le(WrongTopic::getNextReviewDate, LocalDate.now()))
                .orderByAsc(WrongTopic::getNextReviewDate))
            .stream().map(wt -> questionMapper.selectById(wt.getQuestionId())).filter(Objects::nonNull)
            .filter(q -> subjectId == null || subjectId.equals(q.getSubjectId())).limit(limit).map(this::formatQuestion).toList();
    }

    @Override
    public Map<String, Object> updateWrongTopicMastery(Long userId, Long wrongTopicId, boolean isCorrect) {
        WrongTopic wt = wrongTopicMapper.selectById(wrongTopicId);
        if (wt == null || !wt.getUserId().equals(userId)) {
            throw new BusinessException("错题记录不存在");
        }

        wt.setLastReviewTime(LocalDateTime.now());
        wt.setReviewCount((wt.getReviewCount() == null ? 0 : wt.getReviewCount()) + 1);
        wt.setLastReviewResult(isCorrect ? 1 : 0);
        if (isCorrect) {
            wt.setContinuousCorrectCount((wt.getContinuousCorrectCount() == null ? 0 : wt.getContinuousCorrectCount()) + 1);
            int stage = Math.min(5, wt.getContinuousCorrectCount());
            int[] intervals = {1, 3, 7, 14, 30};
            wt.setNextReviewDate(LocalDate.now().plusDays(intervals[stage - 1]));
            if (stage >= 5) {
                wt.setIsMastered(1);
                wt.setMasteryLevel(5);
            } else {
                wt.setMasteryLevel(stage);
            }
        } else {
            wt.setContinuousCorrectCount(0);
            wt.setMasteryLevel(0);
            wt.setTimes((wt.getTimes() == null ? 0 : wt.getTimes()) + 1);
            wt.setLastWrongTime(LocalDateTime.now());
            wt.setNextReviewDate(LocalDate.now().plusDays(1));
        }

        wrongTopicMapper.updateById(wt);

        Map<String, Object> res = new HashMap<>();
        res.put("mastered", Integer.valueOf(1).equals(wt.getIsMastered()));
        res.put("continuousCorrectCount", wt.getContinuousCorrectCount());
        res.put("masteryLevel", wt.getMasteryLevel());
        res.put("nextReviewDate", wt.getNextReviewDate());
        res.put("reviewCount", wt.getReviewCount());
        return res;
    }

    /**
     * 按关卡号从题库中抽题，保证难度递进、题型均匀、题目不重复。
     *
     * 策略：
     * 1. 关卡号映射到目标难度（第一关=难度1，第二关=难度2...，超过5循环）
     * 2. 优先取目标难度的题目；不足时取相邻难度补齐
     * 3. 各题型均匀分配
     *
     * @param allQuestions 该年级段的全部题目
     * @param levelNum     关卡号（1=第一关）
     * @param needCount    本关需要的题目数
     * @param recentIds    最近答过的题目ID（优先排除）
     * @param random       随机源
     */
    private List<Question> pickByLevelSlot(List<Question> allQuestions, int levelNum, int needCount,
                                           Set<Long> recentIds, Random random) {
        if (allQuestions.isEmpty() || needCount <= 0) {
            return new ArrayList<>();
        }

        // 关卡号映射到难度（1-5循环）
        int targetDifficulty = ((levelNum - 1) % 5) + 1;

        // 按难度分组
        Map<Integer, List<Question>> byDifficulty = new HashMap<>();
        for (Question q : allQuestions) {
            int diff = q.getDifficulty() != null ? q.getDifficulty() : 3; // 未评分的默认中等
            byDifficulty.computeIfAbsent(diff, k -> new ArrayList<>()).add(q);
        }

        // 优先取目标难度，不足时按距离取相邻难度
        List<Integer> difficultyOrder = new ArrayList<>();
        difficultyOrder.add(targetDifficulty);
        for (int offset = 1; offset <= 4; offset++) {
            if (targetDifficulty - offset >= 1) difficultyOrder.add(targetDifficulty - offset);
            if (targetDifficulty + offset <= 5) difficultyOrder.add(targetDifficulty + offset);
        }

        List<Question> pool = new ArrayList<>();
        for (int diff : difficultyOrder) {
            List<Question> diffQuestions = byDifficulty.get(diff);
            if (diffQuestions != null && !diffQuestions.isEmpty()) {
                // 每种难度取一部分，保证题型均匀
                Map<Integer, List<Question>> byType = diffQuestions.stream()
                    .collect(Collectors.groupingBy(q -> q.getQuestionType() != null ? q.getQuestionType() : 1));

                for (List<Question> typeQuestions : byType.values()) {
                    List<Question> fresh = typeQuestions.stream()
                        .filter(q -> !recentIds.contains(q.getId()))
                        .collect(Collectors.toList());
                    List<Question> used = typeQuestions.stream()
                        .filter(q -> recentIds.contains(q.getId()))
                        .collect(Collectors.toList());
                    Collections.shuffle(fresh, random);
                    Collections.shuffle(used, random);
                    pool.addAll(fresh);
                    pool.addAll(used);
                }
            }
            // 够了就停
            if (pool.size() >= needCount * 2) break;
        }

        // 去重（同一题可能在不同难度组出现）
        Set<Long> seen = new HashSet<>();
        pool = pool.stream()
            .filter(q -> seen.add(q.getId()))
            .collect(Collectors.toList());

        // 题库不足时，从全部题目补齐
        if (pool.size() < needCount) {
            Set<Long> existIds = pool.stream().map(Question::getId).collect(Collectors.toSet());
            List<Question> shuffled = new ArrayList<>(allQuestions);
            Collections.shuffle(shuffled, random);
            for (Question q : shuffled) {
                if (pool.size() >= needCount) break;
                if (!existIds.contains(q.getId())) {
                    pool.add(q);
                    existIds.add(q.getId());
                }
            }
        }

        List<Question> result = new ArrayList<>();
        for (int i = 0; i < Math.min(needCount, pool.size()); i++) {
            result.add(pool.get(i));
        }
        return result;
    }

    /**
     * 获取用户最近答过的题目ID（用于避免短期内重复出题）
     */
    private Set<Long> getRecentAnsweredQuestionIds(Long userId, Long subjectId, int avoidCount) {
        Set<Long> recentIds = new java.util.HashSet<>();
        try {
            // 从错题本获取用户最近接触的题目（按 lastWrongTime 倒序）
            List<WrongTopic> recentWrong = wrongTopicMapper.selectList(
                new LambdaQueryWrapper<WrongTopic>()
                    .eq(WrongTopic::getUserId, userId)
                    .orderByDesc(WrongTopic::getLastWrongTime)
                    .last("LIMIT " + (avoidCount * 2))
            );
            for (WrongTopic wt : recentWrong) {
                if (wt.getQuestionId() != null) {
                    recentIds.add(wt.getQuestionId());
                }
            }
        } catch (Exception e) {
            log.warn("获取最近答题记录失败: {}", e.getMessage());
        }
        return recentIds;
    }

}
