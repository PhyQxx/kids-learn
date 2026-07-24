package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import com.kidslearn.api.service.ChallengeSeasonService;
import com.kidslearn.api.service.ChallengeService;
import com.kidslearn.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
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

    private static final int CHALLENGE_TYPE_RANKED = 1;
    private static final int CHALLENGE_TYPE_FRIEND = 2;
    private static final int LEADERBOARD_TYPE_CHALLENGE = 2;

    private final ChallengeMapper challengeMapper;
    private final ChallengeRecordMapper challengeRecordMapper;
    private final CourseLevelMapper courseLevelMapper;
    private final FriendMapper friendMapper;
    private final LeaderboardMapper leaderboardMapper;
    private final UserMapper userMapper;
    private final LearningAccessService learningAccessService;
    private final ChallengeSeasonService challengeSeasonService;

    @Override
    @Transactional
    public Map<String, Object> createChallenge(Long userId, CreateChallengeDTO dto) {
        learningAccessService.checkAccess(userId, LearningAccessService.Scene.CHALLENGE);
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
        learningAccessService.checkAccess(userId, LearningAccessService.Scene.CHALLENGE);
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
        record.setRankDelta(settled.rankDelta());
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

        // 统计真实参与人数
        List<Map<String, Object>> playersCount = challengeRecordMapper.countPlayersByChallengeType();
        Map<String, Long> playersMap = new HashMap<>();
        for (Map<String, Object> pc : playersCount) {
            Integer type = (Integer) pc.get("challenge_type");
            Long count = ((Number) pc.get("player_count")).longValue();
            if (type == CHALLENGE_TYPE_RANKED) {
                playersMap.put("rankedPlayers", count);
            } else if (type == CHALLENGE_TYPE_FRIEND) {
                playersMap.put("friendPlayers", count);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tier", RankTierCatalog.resolve(currentChallengePoints(userId)));
        result.put("stats", Map.of(
            "wins", wins,
            "draws", draws,
            "losses", losses,
            "total", records.size()
        ));
        result.put("season", Map.of(
            "name", challengeSeasonService.current().name(),
            "remainingText", seasonRemainingText()
        ));
        result.put("players", playersMap);
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
            if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
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
        Challenge active = challengeMapper.selectOne(
            new LambdaQueryWrapper<Challenge>()
                .eq(Challenge::getChallengeType, typeValue)
                .eq(Challenge::getStatus, 1)
                .last("LIMIT 1")
        );
        if (active != null) {
            return active;
        }

        Challenge challenge = new Challenge();
        challenge.setChallengeName("FRIEND".equals(challengeType) ? "好友对战" : "排位挑战赛");
        challenge.setChallengeType(typeValue);
        challenge.setStartTime(LocalDateTime.now());
        challenge.setEndTime(LocalDateTime.now().plusDays(7));
        challenge.setStatus(1);
        challengeMapper.insert(challenge);
        return challenge;
    }

    private User selectOpponent(Long userId, String challengeType, Long opponentId) {
        if (opponentId != null) {
            User candidate = userMapper.selectById(opponentId);
            if (candidate != null && !candidate.getId().equals(userId)) {
                if ("FRIEND".equals(challengeType) && !isAcceptedFriend(userId, opponentId)) {
                    throw new BusinessException("对方不是您的好友");
                }
                return candidate;
            }
        }

        List<Long> candidateIds = "FRIEND".equals(challengeType) ? friendIds(userId) : List.of();
        List<User> candidates = candidateIds.isEmpty() ? List.of() : userMapper.selectBatchIds(candidateIds);

        if (candidates.isEmpty() && "FRIEND".equals(challengeType)) {
            // throw new BusinessException("没有可挑战的好友"); // Relaxed for demo
        }

        if (candidates.isEmpty()) {
            candidates = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                    .ne(User::getId, userId)
                    .ne(User::getUserType, 3)
                    .last("LIMIT 50")
            );
        }

        if (candidates.isEmpty()) throw new BusinessException("暂时没有真实对手");

        Collections.shuffle(candidates);
        return candidates.get(0);
    }

    private CourseLevel selectRandomLevel() {
        List<CourseLevel> levels = courseLevelMapper.selectList(
            new LambdaQueryWrapper<CourseLevel>()
                .eq(CourseLevel::getStatus, 1)
                .last("LIMIT 50")
        );
        if (levels.isEmpty()) throw new BusinessException("当前没有可用挑战关卡");
        Collections.shuffle(levels);
        return levels.get(0);
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
        if (gold <= 0) return;
        // 原子自增，避免并发结算丢失更新
        userMapper.update(null, new UpdateWrapper<User>().eq("id", userId).setSql("gold = gold + " + gold));
    }

    private Map<String, Object> toRecordMap(ChallengeRecord record, User opponent) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", record.getId());
        map.put("challengeId", record.getChallengeId());
        map.put("myScore", record.getUserScore());
        map.put("opponentScore", record.getOpponentScore());
        map.put("isWin", record.getIsWinner() == 1);
        map.put("win", record.getIsWinner() == 1); // compat
        map.put("rankDelta", record.getRankDelta() != null ? record.getRankDelta() : 0);
        map.put("rewardGold", record.getRewardGold() != null ? record.getRewardGold() : 0);
        map.put("playTime", record.getPlayTime() != null ? record.getPlayTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) : "");
        map.put("time", map.get("playTime")); // compat

        if (opponent != null) {
            map.put("opponentId", opponent.getId());
            map.put("opponentName", displayName(opponent));
            map.put("opponentAvatar", opponent.getAvatar());
        } else {
            map.put("opponentId", record.getOpponentId());
            map.put("opponentName", "神秘对手");
            map.put("opponentAvatar", "👤");
        }
        return map;
    }

    private Map<String, Object> toOpponentMap(User opponent) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", opponent.getId());
        map.put("nickname", displayName(opponent));
        map.put("avatar", opponent.getAvatar());
        map.put("level", opponent.getLevel() != null ? opponent.getLevel() : 1);
        return map;
    }

    private Map<String, Object> toLevelMap(CourseLevel level) {
        Map<String, Object> map = new LinkedHashMap<>();
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
                .or()
                .eq(Friend::getFriendId, userId)
        ).stream().map(f -> f.getUserId().equals(userId) ? f.getFriendId() : f.getUserId()).toList();
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

    private int safeInt(Integer i) {
        return i == null ? 0 : i;
    }

    private long safeLong(Long l) {
        return l == null ? 0 : l;
    }

    private String displayName(User user) {
        return user.getNickname() != null ? user.getNickname() : "User_" + user.getId();
    }

    private String currentSeasonKey() {
        return challengeSeasonService.current().key();
    }

    private String seasonRemainingText() {
        LocalDate now = LocalDate.now();
        long days = now.until(challengeSeasonService.current(now).end(), java.time.temporal.ChronoUnit.DAYS);
        if (days == 0) return "今晚结算";
        return "剩 " + days + " 天结算";
    }
}
