package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.*;
import com.kidslearn.api.mapper.*;
import com.kidslearn.api.service.AchievementService;
import com.kidslearn.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementMapper achievementMapper;
    private final AchievementTierMapper achievementTierMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final StickerMapper stickerMapper;
    private final StickerSeriesMapper stickerSeriesMapper;
    private final UserStickerMapper userStickerMapper;
    private final TitleMapper titleMapper;
    private final UserTitleMapper userTitleMapper;
    private final UserMapper userMapper;
    private final RewardLogMapper rewardLogMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final CourseLevelMapper courseLevelMapper;
    private final CourseMapper courseMapper;
    private final SubjectMapper subjectMapper;

    @Override
    public List<Map<String, Object>> getAchievements(Long userId, Integer type) {
        syncAchievementProgress(userId);

        LambdaQueryWrapper<Achievement> wrapper = new LambdaQueryWrapper<Achievement>()
            .eq(Achievement::getStatus, 1)
            .eq(type != null, Achievement::getAchieveType, type)
            .orderByAsc(Achievement::getSortOrder);
        List<Achievement> achievements = achievementMapper.selectList(wrapper);

        return achievements.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("achieveCode", a.getAchieveCode());
            map.put("achieveName", a.getAchieveName());
            map.put("achieveDesc", a.getAchieveDesc());
            map.put("achieveIcon", a.getAchieveIcon());
            map.put("achieveType", a.getAchieveType());
            map.put("isTiered", a.getIsTiered());

            // user progress
            UserAchievement ua = userAchievementMapper.selectOne(
                new LambdaQueryWrapper<UserAchievement>()
                    .eq(UserAchievement::getUserId, userId)
                    .eq(UserAchievement::getAchieveId, a.getId())
            );
            map.put("currentValue", ua != null ? ua.getCurrentValue() : 0);
            map.put("targetValue", ua != null && ua.getTargetValue() != null ? ua.getTargetValue() : 1);
            map.put("isCompleted", ua != null && ua.getIsCompleted() == 1);
            map.put("isReceived", ua != null && ua.getIsReceived() == 1);

            // tiers
            if (a.getIsTiered() == 1) {
                List<AchievementTier> tiers = achievementTierMapper.selectList(
                    new LambdaQueryWrapper<AchievementTier>()
                        .eq(AchievementTier::getAchieveId, a.getId())
                        .orderByAsc(AchievementTier::getTierLevel)
                );
                List<Map<String, Object>> tierList = tiers.stream().map(t -> {
                    Map<String, Object> tm = new HashMap<>();
                    tm.put("tierLevel", t.getTierLevel());
                    tm.put("tierName", t.getTierName());
                    tm.put("conditionJson", t.getConditionJson());
                    tm.put("rewardJson", t.getRewardJson());
                    return tm;
                }).collect(Collectors.toList());
                map.put("tiers", tierList);
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getMyProgress(Long userId) {
        syncAchievementProgress(userId);

        Map<String, Object> result = new HashMap<>();
        Long total = achievementMapper.selectCount(
            new LambdaQueryWrapper<Achievement>().eq(Achievement::getStatus, 1)
        );
        Long completed = userAchievementMapper.selectCount(
            new LambdaQueryWrapper<UserAchievement>()
                .eq(UserAchievement::getUserId, userId)
                .eq(UserAchievement::getIsCompleted, 1)
        );
        result.put("totalAchievements", total);
        result.put("completedAchievements", completed);

        Long totalStickers = stickerMapper.selectCount(
            new LambdaQueryWrapper<Sticker>().eq(Sticker::getStatus, 1)
        );
        Long myStickerTypes = userStickerMapper.selectCount(
            new LambdaQueryWrapper<UserSticker>().eq(UserSticker::getUserId, userId)
        );
        result.put("totalStickers", totalStickers);
        result.put("collectedStickers", myStickerTypes);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> receiveReward(Long userId, Long achievementId) {
        UserAchievement ua = userAchievementMapper.selectOne(
            new LambdaQueryWrapper<UserAchievement>()
                .eq(UserAchievement::getUserId, userId)
                .eq(UserAchievement::getAchieveId, achievementId)
        );
        if (ua == null || ua.getIsCompleted() != 1) {
            throw new BusinessException("成就未完成");
        }
        if (ua.getIsReceived() == 1) {
            throw new BusinessException("奖励已领取");
        }

        // get tier rewards
        AchievementTier tier = achievementTierMapper.selectOne(
            new LambdaQueryWrapper<AchievementTier>()
                .eq(AchievementTier::getAchieveId, achievementId)
                .eq(AchievementTier::getTierLevel, resolveAchievedTierLevel(achievementId, ua.getCurrentValue()))
                .last("LIMIT 1")
        );

        List<AchievementRuleEngine.RewardItem> rewards = AchievementRuleEngine.resolveRewardItems(
            tier != null ? tier.getRewardJson() : null
        );
        if (rewards.isEmpty()) {
            rewards = List.of(new AchievementRuleEngine.RewardItem("GOLD", null, 50));
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        Map<String, Object> rewardSummary = applyAchievementRewards(user, achievementId, rewards);

        ua.setIsReceived(1);
        userAchievementMapper.updateById(ua);

        Map<String, Object> result = new HashMap<>();
        result.putAll(rewardSummary);
        result.put("message", "奖励领取成功！");
        return result;
    }

    @Override
    public List<Map<String, Object>> getStickers(Long userId, Long seriesId) {
        LambdaQueryWrapper<Sticker> wrapper = new LambdaQueryWrapper<Sticker>()
            .eq(Sticker::getStatus, 1)
            .eq(seriesId != null, Sticker::getSeriesId, seriesId);
        List<Sticker> stickers = stickerMapper.selectList(wrapper);

        List<UserSticker> myStickers = userStickerMapper.selectList(
            new LambdaQueryWrapper<UserSticker>().eq(UserSticker::getUserId, userId)
        );
        Map<Long, Integer> myMap = myStickers.stream()
            .collect(Collectors.toMap(UserSticker::getStickerId, UserSticker::getQuantity, (a, b) -> a));

        return stickers.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("stickerCode", s.getStickerCode());
            map.put("stickerName", s.getStickerName());
            map.put("stickerUrl", s.getStickerUrl());
            map.put("rarity", s.getRarity());
            map.put("seriesName", s.getSeriesName());
            map.put("description", s.getDescription());
            map.put("quantity", myMap.getOrDefault(s.getId(), 0));
            map.put("collected", myMap.containsKey(s.getId()));
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getTitles(Long userId) {
        List<UserTitle> userTitles = userTitleMapper.selectList(
            new LambdaQueryWrapper<UserTitle>().eq(UserTitle::getUserId, userId)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserTitle ut : userTitles) {
            Title title = titleMapper.selectById(ut.getTitleId());
            if (title == null) continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", title.getId());
            map.put("titleCode", title.getTitleCode());
            map.put("titleName", title.getTitleName());
            map.put("titleColor", title.getTitleColor());
            map.put("titleIcon", title.getTitleIcon());
            map.put("isActive", ut.getIsActive());
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> activateTitle(Long userId, Long titleId) {
        UserTitle target = userTitleMapper.selectOne(
            new LambdaQueryWrapper<UserTitle>()
                .eq(UserTitle::getUserId, userId)
                .eq(UserTitle::getTitleId, titleId)
        );
        if (target == null) {
            throw new BusinessException("称号未解锁");
        }

        // deactivate all
        List<UserTitle> all = userTitleMapper.selectList(
            new LambdaQueryWrapper<UserTitle>().eq(UserTitle::getUserId, userId)
        );
        for (UserTitle ut : all) {
            ut.setIsActive(0);
            userTitleMapper.updateById(ut);
        }
        // activate target
        target.setIsActive(1);
        userTitleMapper.updateById(target);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "称号佩戴成功！");
        return result;
    }

    @Override
    public void syncAchievementProgress(Long userId) {
        List<Achievement> achievements = achievementMapper.selectList(
            new LambdaQueryWrapper<Achievement>().eq(Achievement::getStatus, 1)
        );
        if (achievements.isEmpty()) {
            return;
        }

        List<LearningRecord> passedRecords = learningRecordMapper.selectList(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getIsPass, 1)
        );
        Map<Long, Integer> bestStarsByLevel = new HashMap<>();
        for (LearningRecord record : passedRecords) {
            bestStarsByLevel.merge(record.getCourseLevelId(), record.getStars(), Math::max);
        }

        int completedLevels = bestStarsByLevel.size();
        int threeStarLevels = (int) bestStarsByLevel.values().stream().filter(stars -> stars >= 3).count();
        ScoreProgress scoreProgress = countScoreProgress(passedRecords);
        int learnedSubjects = countLearnedSubjects(bestStarsByLevel.keySet());
        int streakDays = countCurrentStreakDays(passedRecords);
        int collectedStickers = Math.toIntExact(userStickerMapper.selectCount(
            new LambdaQueryWrapper<UserSticker>().eq(UserSticker::getUserId, userId)
        ));
        int bestRank = resolveTotalExpRank(userId);
        AchievementRuleEngine.ProgressSnapshot snapshot = new AchievementRuleEngine.ProgressSnapshot(
            completedLevels,
            threeStarLevels,
            scoreProgress.perfectLevels(),
            scoreProgress.mathCompletedLevels(),
            scoreProgress.mathPerfectLevels(),
            learnedSubjects,
            collectedStickers,
            streakDays,
            bestRank
        );

        for (Achievement achievement : achievements) {
            AchievementTier tier = achievementTierMapper.selectOne(
                new LambdaQueryWrapper<AchievementTier>()
                    .eq(AchievementTier::getAchieveId, achievement.getId())
                    .orderByAsc(AchievementTier::getTierLevel)
                    .last("LIMIT 1")
            );
            String conditionJson = tier != null ? tier.getConditionJson() : null;
            int target = AchievementRuleEngine.resolveTarget(
                conditionJson, achievement.getAchieveCode(), achievement.getAchieveName()
            );
            int current = AchievementRuleEngine.resolveCurrent(
                conditionJson, achievement.getAchieveCode(), achievement.getAchieveName(), snapshot
            );
            upsertAchievementProgress(userId, achievement.getId(), current, target);
        }
    }

    private int countLearnedSubjects(Set<Long> completedLevelIds) {
        Set<Long> subjectIds = new HashSet<>();
        for (Long levelId : completedLevelIds) {
            CourseLevel level = courseLevelMapper.selectById(levelId);
            if (level == null) {
                continue;
            }
            Course course = courseMapper.selectById(level.getCourseId());
            if (course != null) {
                subjectIds.add(course.getSubjectId());
            }
        }
        return subjectIds.size();
    }

    private ScoreProgress countScoreProgress(List<LearningRecord> records) {
        Set<Long> perfectLevelIds = new HashSet<>();
        Set<Long> mathLevelIds = new HashSet<>();
        Set<Long> mathPerfectLevelIds = new HashSet<>();

        for (LearningRecord record : records) {
            Long levelId = record.getCourseLevelId();
            CourseLevel level = courseLevelMapper.selectById(levelId);
            if (level == null) {
                continue;
            }

            boolean mathLevel = isMathLevel(level);
            if (mathLevel) {
                mathLevelIds.add(levelId);
            }

            if (isPerfectRecord(record, level)) {
                perfectLevelIds.add(levelId);
                if (mathLevel) {
                    mathPerfectLevelIds.add(levelId);
                }
            }
        }

        return new ScoreProgress(perfectLevelIds.size(), mathLevelIds.size(), mathPerfectLevelIds.size());
    }

    private boolean isPerfectRecord(LearningRecord record, CourseLevel level) {
        Integer totalQuestions = level.getTotalQuestions();
        Integer score = record.getScore();
        if (totalQuestions != null && totalQuestions > 0 && score != null) {
            return score >= totalQuestions * 10;
        }
        return Objects.equals(record.getWrongCount(), 0) && record.getStars() != null && record.getStars() >= 3;
    }

    private boolean isMathLevel(CourseLevel level) {
        Course course = courseMapper.selectById(level.getCourseId());
        if (course == null) {
            return false;
        }
        Subject subject = subjectMapper.selectById(course.getSubjectId());
        return subject != null && "MATH".equalsIgnoreCase(subject.getSubjectCode());
    }

    private record ScoreProgress(int perfectLevels, int mathCompletedLevels, int mathPerfectLevels) {
    }

    private int countCurrentStreakDays(List<LearningRecord> records) {
        Set<LocalDate> learnedDates = records.stream()
            .map(LearningRecord::getPlayTime)
            .filter(Objects::nonNull)
            .map(LocalDateTime::toLocalDate)
            .collect(Collectors.toSet());
        if (learnedDates.isEmpty()) {
            return 0;
        }

        LocalDate cursor = LocalDate.now();
        if (!learnedDates.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }

        int streak = 0;
        while (learnedDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private int resolveTotalExpRank(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getTotalExp() == null) {
            return 0;
        }
        Long betterUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .gt(User::getTotalExp, user.getTotalExp())
        );
        return Math.toIntExact(betterUsers) + 1;
    }

    private int resolveAchievementTarget(Achievement achievement, AchievementTier tier) {
        if (tier != null && tier.getConditionJson() != null) {
            Integer target = firstNumber(tier.getConditionJson(),
                "target", "targetValue", "count", "value", "levelCount", "starCount", "stickerCount", "subjectCount");
            if (target != null && target > 0) {
                return target;
            }
        }

        String code = achievement.getAchieveCode() == null ? "" : achievement.getAchieveCode().toLowerCase(Locale.ROOT);
        String name = achievement.getAchieveName() == null ? "" : achievement.getAchieveName();
        if (code.contains("first") || name.contains("首次") || name.contains("初次")) return 1;
        if (code.contains("star") || name.contains("满星") || name.contains("三星")) return 10;
        if (code.contains("math") || name.contains("数学")) return 50;
        if (code.contains("subject") || name.contains("全科")) return 6;
        if (code.contains("sticker") || name.contains("贴纸") || name.contains("收集")) return 100;
        return 1;
    }

    private int resolveAchievementCurrent(Achievement achievement, int completedLevels, int threeStarLevels, int learnedSubjects, int collectedStickers) {
        String code = achievement.getAchieveCode() == null ? "" : achievement.getAchieveCode().toLowerCase(Locale.ROOT);
        String name = achievement.getAchieveName() == null ? "" : achievement.getAchieveName();

        if (code.contains("sticker") || name.contains("贴纸") || name.contains("收集")) {
            return collectedStickers;
        }
        if (code.contains("subject") || name.contains("全科")) {
            return learnedSubjects;
        }
        if (code.contains("star") || name.contains("满星") || name.contains("三星")) {
            return threeStarLevels;
        }
        return completedLevels;
    }

    private Integer firstNumber(String json, String... keys) {
        for (String key : keys) {
            Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)").matcher(json);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        return null;
    }

    private int resolveAchievedTierLevel(Long achievementId, Integer currentValue) {
        List<AchievementTier> tiers = achievementTierMapper.selectList(
            new LambdaQueryWrapper<AchievementTier>()
                .eq(AchievementTier::getAchieveId, achievementId)
                .orderByAsc(AchievementTier::getTierLevel)
        );
        int safeCurrent = currentValue == null ? 0 : currentValue;
        int achievedLevel = 1;
        for (AchievementTier tier : tiers) {
            int target = AchievementRuleEngine.resolveTarget(tier.getConditionJson(), null, null);
            if (safeCurrent >= Math.max(1, target)) {
                achievedLevel = tier.getTierLevel();
            }
        }
        return achievedLevel;
    }

    private Map<String, Object> applyAchievementRewards(
        User user,
        Long achievementId,
        List<AchievementRuleEngine.RewardItem> rewards
    ) {
        Map<String, Object> summary = new HashMap<>();
        int gold = 0;
        int exp = 0;
        int diamond = 0;
        List<Long> stickers = new ArrayList<>();
        List<Long> titles = new ArrayList<>();

        for (AchievementRuleEngine.RewardItem reward : rewards) {
            switch (reward.type()) {
                case "GOLD" -> gold += reward.quantity();
                case "EXP" -> exp += reward.quantity();
                case "DIAMOND" -> diamond += reward.quantity();
                case "STICKER" -> {
                    if (reward.itemId() != null) {
                        addStickerReward(user.getId(), reward.itemId(), reward.quantity());
                        stickers.add(reward.itemId());
                        insertRewardLog(user.getId(), 4, reward.itemId(), reward.quantity(), achievementId);
                    }
                }
                case "TITLE" -> {
                    if (reward.itemId() != null) {
                        addTitleReward(user.getId(), reward.itemId());
                        titles.add(reward.itemId());
                        insertRewardLog(user.getId(), 5, reward.itemId(), 1, achievementId);
                    }
                }
                default -> {
                }
            }
        }

        if (gold > 0 || exp > 0 || diamond > 0) {
            user.setGold((user.getGold() == null ? 0 : user.getGold()) + gold);
            user.setTotalExp((user.getTotalExp() == null ? 0 : user.getTotalExp()) + exp);
            user.setDiamond((user.getDiamond() == null ? 0 : user.getDiamond()) + diamond);
            userMapper.updateById(user);
        }
        if (gold > 0) {
            insertRewardLog(user.getId(), 1, null, gold, achievementId);
        }
        if (exp > 0) {
            insertRewardLog(user.getId(), 2, null, exp, achievementId);
        }
        if (diamond > 0) {
            insertRewardLog(user.getId(), 3, null, diamond, achievementId);
        }

        summary.put("gold", gold);
        summary.put("exp", exp);
        summary.put("diamond", diamond);
        summary.put("stickers", stickers);
        summary.put("titles", titles);
        return summary;
    }

    private void addStickerReward(Long userId, Long stickerId, int quantity) {
        UserSticker existing = userStickerMapper.selectOne(
            new LambdaQueryWrapper<UserSticker>()
                .eq(UserSticker::getUserId, userId)
                .eq(UserSticker::getStickerId, stickerId)
        );
        if (existing == null) {
            UserSticker userSticker = new UserSticker();
            userSticker.setUserId(userId);
            userSticker.setStickerId(stickerId);
            userSticker.setQuantity(quantity);
            userStickerMapper.insert(userSticker);
            return;
        }
        existing.setQuantity((existing.getQuantity() == null ? 0 : existing.getQuantity()) + quantity);
        userStickerMapper.updateById(existing);
    }

    private void addTitleReward(Long userId, Long titleId) {
        UserTitle existing = userTitleMapper.selectOne(
            new LambdaQueryWrapper<UserTitle>()
                .eq(UserTitle::getUserId, userId)
                .eq(UserTitle::getTitleId, titleId)
        );
        if (existing != null) {
            return;
        }
        UserTitle userTitle = new UserTitle();
        userTitle.setUserId(userId);
        userTitle.setTitleId(titleId);
        userTitle.setIsActive(0);
        userTitle.setObtainTime(LocalDateTime.now());
        userTitleMapper.insert(userTitle);
    }

    private void insertRewardLog(Long userId, int rewardType, Long rewardItemId, int quantity, Long achievementId) {
        RewardLog log = new RewardLog();
        log.setUserId(userId);
        log.setRewardType(rewardType);
        log.setRewardItemId(rewardItemId);
        log.setQuantity(quantity);
        log.setSourceType("ACHIEVEMENT");
        log.setSourceId(achievementId);
        log.setDescription("鎴愬氨濂栧姳");
        rewardLogMapper.insert(log);
    }

    private void upsertAchievementProgress(Long userId, Long achievementId, int current, int target) {
        UserAchievement existing = userAchievementMapper.selectOne(
            new LambdaQueryWrapper<UserAchievement>()
                .eq(UserAchievement::getUserId, userId)
                .eq(UserAchievement::getAchieveId, achievementId)
        );

        int safeTarget = Math.max(1, target);
        int safeCurrent = Math.max(0, current);
        int completed = safeCurrent >= safeTarget ? 1 : 0;

        if (existing == null) {
            UserAchievement ua = new UserAchievement();
            ua.setUserId(userId);
            ua.setAchieveId(achievementId);
            ua.setCurrentValue(safeCurrent);
            ua.setTargetValue(safeTarget);
            ua.setIsCompleted(completed);
            ua.setIsReceived(0);
            if (completed == 1) {
                ua.setCompletedTime(LocalDateTime.now());
            }
            userAchievementMapper.insert(ua);
            return;
        }

        boolean newlyCompleted = existing.getIsCompleted() == null || existing.getIsCompleted() == 0;
        existing.setCurrentValue(safeCurrent);
        existing.setTargetValue(safeTarget);
        existing.setIsCompleted(completed);
        if (completed == 1 && newlyCompleted) {
            existing.setCompletedTime(LocalDateTime.now());
        }
        if (existing.getIsReceived() == null) {
            existing.setIsReceived(0);
        }
        userAchievementMapper.updateById(existing);
    }
}
