package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.dto.challenge.CreateChallengeDTO;
import com.kidslearn.api.dto.challenge.SubmitChallengeDTO;
import com.kidslearn.api.entity.Challenge;
import com.kidslearn.api.entity.ChallengeRecord;
import com.kidslearn.api.entity.CourseLevel;
import com.kidslearn.api.entity.Friend;
import com.kidslearn.api.entity.Leaderboard;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.ChallengeMapper;
import com.kidslearn.api.mapper.ChallengeRecordMapper;
import com.kidslearn.api.mapper.CourseLevelMapper;
import com.kidslearn.api.mapper.FriendMapper;
import com.kidslearn.api.mapper.LeaderboardMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.api.service.ChallengeService;
import com.kidslearn.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

    private static final int CHALLENGE_TYPE_FRIEND = 1;
    private static final int CHALLENGE_TYPE_RANKED = 2;
    private static final int CHALLENGE_STATUS_ACTIVE = 1;
    private static final int LEADERBOARD_TYPE_CHALLENGE = 4;

    private final ChallengeMapper challengeMapper;
    private final ChallengeRecordMapper challengeRecordMapper;
    private final CourseLevelMapper courseLevelMapper;
    private final FriendMapper friendMapper;
    private final LeaderboardMapper leaderboardMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public Map<String, Object> createChallenge(Long userId, CreateChallengeDTO dto) {
        String challengeType = normalizeType(dto != null ? dto.getType() : null);
        Challenge challenge = ensureActiveChallenge(challengeType);
        User opponent = selectOpponent(userId, challengeType, dto != null ? dto.getOpponentId() : null);
        CourseLevel level = selectRandomLevel();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("challengeId", challenge.getId());
        result.put("type", challengeType);
        result.put("challengeName", challenge.getChallengeName());
        result.put("opponent", toOpponentMap(opponent));
        result.put("level", toLevelMap(level));
        result.put("tier", RankTierCatalog.resolve(currentChallengePoints(userId)));
        result.put("message", "挑战已创建");
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> submitChallengeResult(Long userId, SubmitChallengeDTO dto) {
        if (dto == null || dto.getChallengeId() == null) {
            throw new BusinessException("挑战不存在");
        }
        Challenge challenge = challengeMapper.selectById(dto.getChallengeId());
        if (challenge == null) {
            throw new BusinessException("挑战不存在");
        }

        User opponent = dto.getOpponentId() != null ? userMapper.selectById(dto.getOpponentId()) : null;
        int userScore = Math.max(0, dto.getUserScore() == null ? 0 : dto.getUserScore());
        int opponentScore = dto.getOpponentScore() != null
            ? Math.max(0, dto.getOpponentScore())
            : simulateOpponentScore(opponent, userScore);

        ChallengeResultEngine.Result settled = ChallengeResultEngine.settle(userScore, opponentScore);
        ChallengeRecord record = new ChallengeRecord();
        record.setChallengeId(challenge.getId());
        record.setUserId(userId);
        record.setOpponentId(opponent != null ? opponent.getId() : null);
        record.setUserScore(userScore);
        record.setOpponentScore(opponentScore);
        record.setIsWinner(settled.isWinner());
        record.setRewardGold(settled.rewardGold());
        record.setPlayTime(LocalDateTime.now());
        challengeRecordMapper.insert(record);

        addGold(userId, settled.rewardGold());
        long points = addChallengePoints(userId, settled.rankDelta());

        Map<String, Object> result = toRecordMap(record, opponent);
        result.put("rankDelta", settled.rankDelta());
        result.put("tier", RankTierCatalog.resolve(points));
        return result;
    }

    @Override
    public Map<String, Object> getDashboard(Long userId) {
        List<ChallengeRecord> records = challengeRecordMapper.selectList(
            new LambdaQueryWrapper<ChallengeRecord>()
                .eq(ChallengeRecord::getUserId, userId)
                .orderByDesc(ChallengeRecord::getCreateTime)
                .last("LIMIT 50")
        );
        long wins = records.stream().filter(record -> Objects.equals(record.getIsWinner(), 1)).count();
        long draws = records.stream().filter(record -> Objects.equals(record.getIsWinner(), 2)).count();
        long losses = records.stream().filter(record -> Objects.equals(record.getIsWinner(), 0)).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tier", RankTierCatalog.resolve(currentChallengePoints(userId)));
        result.put("stats", Map.of(
            "wins", wins,
            "draws", draws,
            "losses", losses,
            "total", records.size()
        ));
        result.put("season", Map.of(
            "name", "第 " + currentWeekNumber() + " 周挑战赛",
            "remainingText", seasonRemainingText()
        ));
        return result;
    }

    @Override
    public List<Map<String, Object>> getChallengeRecords(Long userId) {
        List<ChallengeRecord> records = challengeRecordMapper.selectList(
            new LambdaQueryWrapper<ChallengeRecord>()
                .eq(ChallengeRecord::getUserId, userId)
                .orderByDesc(ChallengeRecord::getCreateTime)
                .last("LIMIT 20")
        );

        return records.stream()
            .map(record -> toRecordMap(record, record.getOpponentId() != null ? userMapper.selectById(record.getOpponentId()) : null))
            .toList();
    }

    @Override
    public List<Map<String, Object>> getChallengeRanking(Long userId) {
        List<Leaderboard> rows = leaderboardMapper.selectList(
            new LambdaQueryWrapper<Leaderboard>()
                .eq(Leaderboard::getRankType, LEADERBOARD_TYPE_CHALLENGE)
                .eq(Leaderboard::getRankWeek, currentSeasonKey())
                .orderByDesc(Leaderboard::getRankValue)
                .last("LIMIT 50")
        );

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        boolean containsMe = false;
        for (Leaderboard row : rows) {
            User user = userMapper.selectById(row.getUserId());
            if (user == null) {
                continue;
            }
            containsMe = containsMe || user.getId().equals(userId);
            result.add(toRankingMap(user, safeLong(row.getRankValue()), rank++, user.getId().equals(userId)));
        }

        if (!containsMe) {
            long myPoints = currentChallengePoints(userId);
            User me = userMapper.selectById(userId);
            if (me != null) {
                long ahead = leaderboardMapper.selectCount(
                    new LambdaQueryWrapper<Leaderboard>()
                        .eq(Leaderboard::getRankType, LEADERBOARD_TYPE_CHALLENGE)
                        .eq(Leaderboard::getRankWeek, currentSeasonKey())
                        .gt(Leaderboard::getRankValue, myPoints)
                );
                result.add(toRankingMap(me, myPoints, (int) ahead + 1, true));
            }
        }
        return result;
    }

    private Challenge ensureActiveChallenge(String challengeType) {
        int typeValue = "FRIEND".equals(challengeType) ? CHALLENGE_TYPE_FRIEND : CHALLENGE_TYPE_RANKED;
        Challenge challenge = challengeMapper.selectOne(
            new LambdaQueryWrapper<Challenge>()
                .eq(Challenge::getChallengeType, typeValue)
                .eq(Challenge::getStatus, CHALLENGE_STATUS_ACTIVE)
                .last("LIMIT 1")
        );
        if (challenge != null) {
            return challenge;
        }

        Challenge created = new Challenge();
        created.setChallengeName(typeValue == CHALLENGE_TYPE_FRIEND ? "好友知识PK" : "段位排位赛");
        created.setChallengeType(typeValue);
        created.setStartTime(LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay());
        created.setEndTime(LocalDate.now().with(DayOfWeek.SUNDAY).atTime(23, 59, 59));
        created.setStatus(CHALLENGE_STATUS_ACTIVE);
        challengeMapper.insert(created);
        return created;
    }

    private User selectOpponent(Long userId, String challengeType, Long opponentId) {
        if (opponentId != null && !opponentId.equals(userId)) {
            User explicit = userMapper.selectById(opponentId);
            if (explicit == null) {
                throw new BusinessException("对手不存在");
            }
            if ("FRIEND".equals(challengeType) && !isAcceptedFriend(userId, opponentId)) {
                throw new BusinessException("只能挑战已添加的好友");
            }
            return explicit;
        }

        List<Long> candidateIds = "FRIEND".equals(challengeType) ? friendIds(userId) : List.of();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
            .eq(User::getStatus, 1)
            .ne(User::getId, userId)
            .last("LIMIT 30");
        if (!candidateIds.isEmpty()) {
            wrapper.in(User::getId, candidateIds);
        }
        List<User> candidates = userMapper.selectList(wrapper);
        if (candidates.isEmpty() && "FRIEND".equals(challengeType)) {
            candidates = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                    .eq(User::getStatus, 1)
                    .ne(User::getId, userId)
                    .last("LIMIT 30")
            );
        }
        if (candidates.isEmpty()) {
            return null;
        }
        User current = userMapper.selectById(userId);
        int currentExp = current != null && current.getTotalExp() != null ? current.getTotalExp() : 0;
        return candidates.stream()
            .min(Comparator.comparingInt(user -> Math.abs((user.getTotalExp() == null ? 0 : user.getTotalExp()) - currentExp)))
            .orElse(candidates.get(0));
    }

    private CourseLevel selectRandomLevel() {
        List<CourseLevel> levels = courseLevelMapper.selectList(
            new LambdaQueryWrapper<CourseLevel>()
                .eq(CourseLevel::getStatus, 1)
                .eq(CourseLevel::getIsUnlock, 1)
                .last("ORDER BY RAND() LIMIT 1")
        );
        return levels.isEmpty() ? null : levels.get(0);
    }

    private long addChallengePoints(Long userId, int delta) {
        Leaderboard row = leaderboardMapper.selectOne(
            new LambdaQueryWrapper<Leaderboard>()
                .eq(Leaderboard::getUserId, userId)
                .eq(Leaderboard::getRankType, LEADERBOARD_TYPE_CHALLENGE)
                .eq(Leaderboard::getRankWeek, currentSeasonKey())
                .last("LIMIT 1")
        );
        long points = Math.max(0, (row != null ? safeLong(row.getRankValue()) : 0) + delta);
        if (row == null) {
            row = new Leaderboard();
            row.setUserId(userId);
            row.setRankType(LEADERBOARD_TYPE_CHALLENGE);
            row.setRankWeek(currentSeasonKey());
            row.setRankValue(points);
            leaderboardMapper.insert(row);
        } else {
            row.setRankValue(points);
            leaderboardMapper.updateById(row);
        }
        return points;
    }

    private long currentChallengePoints(Long userId) {
        Leaderboard row = leaderboardMapper.selectOne(
            new LambdaQueryWrapper<Leaderboard>()
                .eq(Leaderboard::getUserId, userId)
                .eq(Leaderboard::getRankType, LEADERBOARD_TYPE_CHALLENGE)
                .eq(Leaderboard::getRankWeek, currentSeasonKey())
                .last("LIMIT 1")
        );
        return row == null ? 0 : safeLong(row.getRankValue());
    }

    private void addGold(Long userId, int gold) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        user.setGold((user.getGold() == null ? 0 : user.getGold()) + gold);
        userMapper.updateById(user);
    }

    private Map<String, Object> toRecordMap(ChallengeRecord record, User opponent) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", record.getId());
        map.put("challengeId", record.getChallengeId());
        map.put("opponentId", record.getOpponentId());
        map.put("opponentName", opponent != null ? opponent.getNickname() : "随机对手");
        map.put("opponentAvatar", opponent != null ? opponent.getAvatar() : "boy");
        map.put("myScore", record.getUserScore());
        map.put("opponentScore", record.getOpponentScore());
        map.put("isWin", Objects.equals(record.getIsWinner(), 1));
        map.put("isWinner", record.getIsWinner());
        map.put("rewardGold", record.getRewardGold());
        map.put("rankDelta", rankDeltaFor(record.getIsWinner()));
        map.put("playTime", record.getPlayTime() != null ? record.getPlayTime().toString() : null);
        return map;
    }

    private Map<String, Object> toOpponentMap(User user) {
        if (user == null) {
            return Map.of("id", 0, "nickname", "星球挑战者", "avatar", "boy", "level", 1);
        }
        return Map.of(
            "id", user.getId(),
            "nickname", displayName(user),
            "avatar", user.getAvatar() != null ? user.getAvatar() : "boy",
            "level", user.getLevel() != null ? user.getLevel() : 1
        );
    }

    private Map<String, Object> toLevelMap(CourseLevel level) {
        if (level == null) {
            return Map.of();
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", level.getId());
        map.put("levelName", level.getLevelName());
        map.put("levelNo", level.getLevelNum());
        return map;
    }

    private Map<String, Object> toRankingMap(User user, long score, int rank, boolean isMe) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("nickname", displayName(user));
        map.put("avatar", user.getAvatar());
        map.put("level", user.getLevel() != null ? user.getLevel() : 1);
        map.put("score", score);
        map.put("rank", rank);
        map.put("isMe", isMe);
        map.put("tier", RankTierCatalog.resolve(score).tierName());
        return map;
    }

    private List<Long> friendIds(Long userId) {
        return friendMapper.selectList(
            new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getStatus, 1)
        ).stream().map(Friend::getFriendId).toList();
    }

    private String displayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return "星球挑战者";
    }

    private boolean isAcceptedFriend(Long userId, Long friendId) {
        Long count = friendMapper.selectCount(
            new LambdaQueryWrapper<Friend>()
                .eq(Friend::getStatus, 1)
                .and(wrapper -> wrapper
                    .eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId)
                    .or()
                    .eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId))
        );
        return count != null && count > 0;
    }

    private int simulateOpponentScore(User opponent, int userScore) {
        int levelBonus = opponent != null && opponent.getLevel() != null ? Math.min(20, opponent.getLevel() * 2) : 8;
        int variance = ThreadLocalRandom.current().nextInt(-12, 13);
        return Math.max(0, Math.min(100, userScore - 5 + levelBonus + variance));
    }

    private String normalizeType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return "RANKED";
        }
        String upper = rawType.trim().toUpperCase(Locale.ROOT);
        return "FRIEND".equals(upper) || "FRIENDS".equals(upper) ? "FRIEND" : "RANKED";
    }

    private String currentSeasonKey() {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;
        return today.get(weekFields.weekBasedYear()) + "-W" + String.format("%02d", today.get(weekFields.weekOfWeekBasedYear()));
    }

    private int currentWeekNumber() {
        return LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear());
    }

    private String seasonRemainingText() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = LocalDate.now().with(DayOfWeek.SUNDAY).atTime(23, 59, 59);
        Duration duration = Duration.between(now, end);
        if (duration.isNegative()) {
            return "即将开启新赛季";
        }
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        return days + "天 " + hours + "小时";
    }

    private long safeLong(Long value) {
        return value == null ? 0 : value;
    }

    private int rankDeltaFor(Integer isWinner) {
        if (Objects.equals(isWinner, 1)) {
            return 30;
        }
        if (Objects.equals(isWinner, 2)) {
            return 10;
        }
        return -12;
    }
}
