package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidslearn.api.dto.challenge.CreateChallengeDTO;
import com.kidslearn.api.dto.challenge.SubmitChallengeAnswerDTO;
import com.kidslearn.api.entity.*;
import com.kidslearn.api.mapper.*;
import com.kidslearn.api.service.AsyncChallengeService;
import com.kidslearn.api.service.NotificationEventService;
import com.kidslearn.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AsyncChallengeServiceImpl implements AsyncChallengeService {
    private static final int QUESTION_COUNT = 10;
    private static final int RANK_TYPE = 2;

    private final ChallengeMatchMapper matchMapper;
    private final ChallengeParticipantMapper participantMapper;
    private final ChallengeQuestionSnapshotMapper snapshotMapper;
    private final ChallengeAnswerRecordMapper answerMapper;
    private final ChallengeRewardLogMapper rewardMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;
    private final FriendMapper friendMapper;
    private final UserMapper userMapper;
    private final LeaderboardMapper leaderboardMapper;
    private final LearningAccessService learningAccessService;
    private final ObjectMapper objectMapper;
    private final NotificationEventService notificationEventService;

    @Override
    @Transactional
    public Map<String, Object> createOrJoin(Long userId, CreateChallengeDTO dto) {
        learningAccessService.checkAccess(userId, LearningAccessService.Scene.CHALLENGE);
        String type = normalizeType(dto == null ? null : dto.getType());
        if ("FRIEND".equals(type)) {
            Long opponentId = dto == null ? null : dto.getOpponentId();
            if (opponentId == null || opponentId.equals(userId)) throw new BusinessException("请选择要挑战的好友");
            User opponent = userMapper.selectById(opponentId);
            if (opponent == null || Integer.valueOf(0).equals(opponent.getStatus())) throw new BusinessException("该好友账号不可用");
            if (!isAcceptedFriend(userId, opponentId)) throw new BusinessException("对方不是您的已确认好友");
            return create(userId, opponentId, type, true);
        }

        ChallengeMatch waiting = matchMapper.selectOne(new LambdaQueryWrapper<ChallengeMatch>()
            .eq(ChallengeMatch::getMatchType, type).eq(ChallengeMatch::getStatus, "WAITING")
            .isNull(ChallengeMatch::getOpponentId).ne(ChallengeMatch::getCreatorId, userId)
            .gt(ChallengeMatch::getExpiresAt, LocalDateTime.now()).orderByAsc(ChallengeMatch::getCreateTime).last("LIMIT 1"));
        if (waiting != null) {
            User creator = userMapper.selectById(waiting.getCreatorId());
            if (creator == null || Integer.valueOf(0).equals(creator.getStatus())) waiting = null;
        }
        if (waiting != null) {
            int updated = matchMapper.update(null, new LambdaUpdateWrapper<ChallengeMatch>()
                .eq(ChallengeMatch::getId, waiting.getId()).eq(ChallengeMatch::getStatus, "WAITING")
                .isNull(ChallengeMatch::getOpponentId).set(ChallengeMatch::getOpponentId, userId)
                .set(ChallengeMatch::getStatus, "ACTIVE"));
            if (updated == 1) {
                insertParticipant(waiting.getId(), userId, "ACTIVE");
                return response(waiting.getId(), userId, "已匹配真实对手");
            }
        }
        return create(userId, null, type, false);
    }

    private Map<String, Object> create(Long userId, Long opponentId, String type, boolean invited) {
        ChallengeSeasonCatalog.Season season = ChallengeSeasonCatalog.current(LocalDate.now());
        ChallengeMatch match = new ChallengeMatch();
        match.setMatchType(type); match.setCreatorId(userId); match.setOpponentId(opponentId);
        match.setStatus(invited ? "INVITED" : "WAITING"); match.setSeasonKey(season.key());
        match.setRuleSnapshot("{\"questionCount\":10,\"trustedScoring\":true}");
        match.setExpiresAt(LocalDateTime.now().plusHours(48)); match.setVersion(0);
        matchMapper.insert(match);
        insertParticipant(match.getId(), userId, "ACTIVE");
        if (opponentId != null) insertParticipant(match.getId(), opponentId, "INVITED");
        createSnapshots(match.getId());
        if (invited) notificationEventService.publish("challenge-invite:" + match.getId() + ":" + opponentId,
            opponentId, "CHALLENGE_INVITE", "好友挑战邀请", "好友向你发起了异步成绩挑战",
            "OPEN_CHALLENGE", String.valueOf(match.getId()), match.getExpiresAt());
        return response(match.getId(), userId, invited ? "好友挑战邀请已发送" : "成绩提交后将等待真实用户接力");
    }

    private void createSnapshots(Long matchId) {
        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>().last("ORDER BY RAND() LIMIT " + QUESTION_COUNT));
        if (questions.isEmpty()) throw new BusinessException("当前题库暂无可用题目");
        int sequence = 1;
        for (Question question : questions) {
            List<QuestionOption> options = optionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, question.getId()).orderByAsc(QuestionOption::getSortOrder));
            QuestionAnswerEvaluator.Evaluation evaluation = QuestionAnswerEvaluator.evaluate(question, options, "__snapshot__");
            ChallengeQuestionSnapshot snapshot = new ChallengeQuestionSnapshot();
            snapshot.setMatchId(matchId); snapshot.setQuestionId(question.getId()); snapshot.setSequenceNo(sequence++);
            snapshot.setScore(question.getScore() == null ? 10 : question.getScore());
            snapshot.setQuestionContent(question.getQuestionContent()); snapshot.setQuestionType(question.getQuestionType());
            snapshot.setOptionsJson(writeOptions(options)); snapshot.setCorrectAnswer(evaluation.correctAnswer());
            snapshotMapper.insert(snapshot);
        }
    }

    private String writeOptions(List<QuestionOption> options) {
        List<Map<String, Object>> safe = options.stream().map(o -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", o.getId()); value.put("optionLabel", o.getOptionLabel());
            value.put("optionContent", o.getOptionContent()); value.put("sortOrder", o.getSortOrder());
            return value;
        }).toList();
        try { return objectMapper.writeValueAsString(safe); }
        catch (Exception e) { throw new BusinessException("题目快照生成失败"); }
    }

    @Override
    public List<Map<String, Object>> questions(Long userId, Long matchId) {
        requireActiveParticipant(userId, matchId);
        return snapshotMapper.selectList(new LambdaQueryWrapper<ChallengeQuestionSnapshot>()
                .eq(ChallengeQuestionSnapshot::getMatchId, matchId).orderByAsc(ChallengeQuestionSnapshot::getSequenceNo))
            .stream().map(this::questionMap).toList();
    }

    private Map<String, Object> questionMap(ChallengeQuestionSnapshot q) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", q.getId());
        map.put("snapshotId", q.getId()); map.put("sourceQuestionId", q.getQuestionId());
        map.put("questionContent", q.getQuestionContent()); map.put("questionType", q.getQuestionType());
        map.put("score", q.getScore());
        try { map.put("options", objectMapper.readValue(q.getOptionsJson(), new TypeReference<List<Map<String, Object>>>() {})); }
        catch (Exception e) { map.put("options", List.of()); }
        return map;
    }

    @Override
    @Transactional
    public Map<String, Object> submitAnswer(Long userId, Long matchId, SubmitChallengeAnswerDTO dto) {
        learningAccessService.checkAccess(userId, LearningAccessService.Scene.CHALLENGE);
        ChallengeParticipant participant = requireActiveParticipant(userId, matchId);
        ChallengeQuestionSnapshot snapshot = snapshotMapper.selectById(dto.getSnapshotId());
        if (snapshot == null || !matchId.equals(snapshot.getMatchId())) throw new BusinessException("题目不属于本场挑战");
        // 挑战已结束时，迟到/重复的提交静默幂等返回，不再计分也不打扰用户
        if ("COMPLETED".equals(participant.getStatus())) {
            ChallengeAnswerRecord existing = answerMapper.selectOne(new LambdaQueryWrapper<ChallengeAnswerRecord>()
                .eq(ChallengeAnswerRecord::getMatchId, matchId).eq(ChallengeAnswerRecord::getUserId, userId)
                .eq(ChallengeAnswerRecord::getSnapshotId, snapshot.getId()).last("LIMIT 1"));
            if (existing != null) {
                return Map.of("correct", Objects.equals(existing.getIsCorrect(), 1),
                    "awardedScore", safe(existing.getAwardedScore()), "correctAnswer", snapshot.getCorrectAnswer());
            }
            return Map.of("correct", false, "awardedScore", 0, "correctAnswer", snapshot.getCorrectAnswer());
        }
        boolean correct = ChallengeSnapshotEvaluator.correct(snapshot.getQuestionType(), snapshot.getCorrectAnswer(), dto.getAnswer());
        ChallengeAnswerRecord record = new ChallengeAnswerRecord();
        record.setMatchId(matchId); record.setUserId(userId); record.setSnapshotId(snapshot.getId()); record.setAnswer(dto.getAnswer());
        record.setIsCorrect(correct ? 1 : 0); record.setAwardedScore(correct ? snapshot.getScore() : 0);
        record.setDurationMs(dto.getDurationMs() == null ? 0 : Math.min(dto.getDurationMs(), 3_600_000));
        try { answerMapper.insert(record); }
        catch (DuplicateKeyException e) { throw new BusinessException("本题已经提交，不能重复计分"); }
        return Map.of("correct", correct, "awardedScore", record.getAwardedScore(), "correctAnswer", snapshot.getCorrectAnswer());
    }

    @Override
    @Transactional
    public Map<String, Object> finish(Long userId, Long matchId) {
        ChallengeParticipant participant = requireActiveParticipant(userId, matchId);
        if (!"COMPLETED".equals(participant.getStatus())) {
            List<ChallengeAnswerRecord> answers = answerMapper.selectList(new LambdaQueryWrapper<ChallengeAnswerRecord>()
                .eq(ChallengeAnswerRecord::getMatchId, matchId).eq(ChallengeAnswerRecord::getUserId, userId));
            participant.setScore(answers.stream().mapToInt(v -> safe(v.getAwardedScore())).sum());
            participant.setCorrectCount((int) answers.stream().filter(v -> Objects.equals(v.getIsCorrect(), 1)).count());
            participant.setDurationMs(answers.stream().mapToLong(v -> v.getDurationMs() == null ? 0 : v.getDurationMs()).sum());
            participant.setStatus("COMPLETED"); participant.setCompletedAt(LocalDateTime.now()); participantMapper.updateById(participant);
        }
        settleIfReady(matchId);
        return status(userId, matchId);
    }

    private void settleIfReady(Long matchId) {
        ChallengeMatch match = requireMatch(matchId);
        List<ChallengeParticipant> participants = participants(matchId);
        if (participants.size() < 2 || participants.stream().anyMatch(p -> !"COMPLETED".equals(p.getStatus()))) {
            matchMapper.update(null, new LambdaUpdateWrapper<ChallengeMatch>().eq(ChallengeMatch::getId, matchId)
                .ne(ChallengeMatch::getStatus, "SETTLED").set(ChallengeMatch::getStatus, "AWAITING_OPPONENT"));
            return;
        }
        int claimed = matchMapper.update(null, new LambdaUpdateWrapper<ChallengeMatch>().eq(ChallengeMatch::getId, matchId)
            .ne(ChallengeMatch::getStatus, "SETTLED").set(ChallengeMatch::getStatus, "SETTLED")
            .set(ChallengeMatch::getSettledAt, LocalDateTime.now()));
        if (claimed == 0) return;
        ChallengeParticipant first = participants.get(0), second = participants.get(1);
        reward(match, first, second); reward(match, second, first);
        notificationEventService.publish("challenge-result:" + matchId + ":" + first.getUserId(), first.getUserId(),
            "CHALLENGE_RESULT", "挑战结果已出", resultContent(first, second), "OPEN_CHALLENGE", String.valueOf(matchId), LocalDateTime.now().plusDays(30));
        notificationEventService.publish("challenge-result:" + matchId + ":" + second.getUserId(), second.getUserId(),
            "CHALLENGE_RESULT", "挑战结果已出", resultContent(second, first), "OPEN_CHALLENGE", String.valueOf(matchId), LocalDateTime.now().plusDays(30));
    }

    private String resultContent(ChallengeParticipant me, ChallengeParticipant other) {
        int compare = Integer.compare(safe(me.getScore()), safe(other.getScore()));
        return "本场得分 " + safe(me.getScore()) + "，" + (compare > 0 ? "挑战胜利" : compare == 0 ? "双方平局" : "继续加油");
    }

    private void reward(ChallengeMatch match, ChallengeParticipant current, ChallengeParticipant other) {
        int comparison = Integer.compare(safe(current.getScore()), safe(other.getScore()));
        int gold = "FRIEND".equals(match.getMatchType())
            ? (comparison > 0 ? 10 : comparison == 0 ? 6 : 3)
            : (comparison > 0 ? 20 : comparison == 0 ? 8 : 5);
        int rank = comparison > 0 ? 30 : comparison == 0 ? 10 : -12;
        if (!eligibleGold(match, current.getUserId(), other.getUserId())) gold = 0;
        if (gold > 0) { insertReward(match.getId(), current.getUserId(), "GOLD", gold); addGold(current.getUserId(), gold); }
        if (!"FRIEND".equals(match.getMatchType()) && dailyRewardCount(current.getUserId(), "RANK") < 10) {
            insertReward(match.getId(), current.getUserId(), "RANK", rank); addRank(current.getUserId(), match.getSeasonKey(), rank);
        }
    }

    private boolean eligibleGold(ChallengeMatch match, Long userId, Long otherId) {
        if (!"FRIEND".equals(match.getMatchType())) return dailyRewardCount(userId, "GOLD") < 5;
        List<ChallengeRewardLog> today = todayRewards(userId, "GOLD");
        if (today.size() >= 3) return false;
        for (ChallengeRewardLog log : today) {
            ChallengeMatch old = matchMapper.selectById(log.getMatchId());
            if (old != null && (otherId.equals(old.getCreatorId()) || otherId.equals(old.getOpponentId()))) return false;
        }
        return true;
    }

    private int dailyRewardCount(Long userId, String type) { return todayRewards(userId, type).size(); }
    private List<ChallengeRewardLog> todayRewards(Long userId, String type) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return rewardMapper.selectList(new LambdaQueryWrapper<ChallengeRewardLog>().eq(ChallengeRewardLog::getUserId, userId)
            .eq(ChallengeRewardLog::getRewardType, type).ge(ChallengeRewardLog::getCreateTime, start));
    }

    private void insertReward(Long matchId, Long userId, String type, int amount) {
        ChallengeRewardLog log = new ChallengeRewardLog(); log.setMatchId(matchId); log.setUserId(userId);
        log.setRewardType(type); log.setAmount(amount); rewardMapper.insert(log);
    }

    private void addGold(Long userId, int amount) {
        User user = userMapper.selectById(userId); if (user == null) return;
        user.setGold(safe(user.getGold()) + amount); userMapper.updateById(user);
    }

    private void addRank(Long userId, String season, int delta) {
        Leaderboard row = leaderboardMapper.selectOne(new LambdaQueryWrapper<Leaderboard>().eq(Leaderboard::getUserId, userId)
            .eq(Leaderboard::getRankType, RANK_TYPE).eq(Leaderboard::getRankWeek, season).last("LIMIT 1"));
        if (row == null) { row = new Leaderboard(); row.setUserId(userId); row.setRankType(RANK_TYPE); row.setRankWeek(season); row.setRankValue((long) Math.max(0, delta)); leaderboardMapper.insert(row); }
        else { row.setRankValue(Math.max(0, (row.getRankValue() == null ? 0 : row.getRankValue()) + delta)); leaderboardMapper.updateById(row); }
    }

    @Override
    public Map<String, Object> status(Long userId, Long matchId) {
        ChallengeMatch match = requireMatch(matchId); ChallengeParticipant me = requireParticipant(userId, matchId);
        List<ChallengeParticipant> participants = participants(matchId);
        ChallengeParticipant other = participants.stream().filter(p -> !p.getUserId().equals(userId)).findFirst().orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("matchId", matchId); result.put("challengeId", matchId); result.put("type", match.getMatchType());
        result.put("status", match.getStatus()); result.put("participantStatus", me.getStatus());
        result.put("myScore", me.getScore()); result.put("opponentScore", other == null ? null : other.getScore());
        result.put("settled", "SETTLED".equals(match.getStatus()));
        if (other != null) result.put("opponent", opponentMap(other.getUserId()));
        if ("SETTLED".equals(match.getStatus()) && other != null) {
            int compare = Integer.compare(safe(me.getScore()), safe(other.getScore()));
            result.put("isWin", compare > 0); result.put("result", compare > 0 ? "WIN" : compare == 0 ? "DRAW" : "LOSS");
            result.put("rewardGold", rewardAmount(matchId, userId, "GOLD")); result.put("rankDelta", rewardAmount(matchId, userId, "RANK"));
        }
        return result;
    }

    private int rewardAmount(Long matchId, Long userId, String type) {
        ChallengeRewardLog log = rewardMapper.selectOne(new LambdaQueryWrapper<ChallengeRewardLog>().eq(ChallengeRewardLog::getMatchId, matchId)
            .eq(ChallengeRewardLog::getUserId, userId).eq(ChallengeRewardLog::getRewardType, type).last("LIMIT 1"));
        return log == null ? 0 : log.getAmount();
    }

    @Override
    @Transactional
    public Map<String, Object> accept(Long userId, Long matchId) {
        ChallengeMatch match = requireMatch(matchId); ChallengeParticipant participant = requireParticipant(userId, matchId);
        if (!userId.equals(match.getOpponentId()) || !"INVITED".equals(participant.getStatus())) throw new BusinessException("没有可接受的好友邀请");
        participant.setStatus("ACTIVE"); participantMapper.updateById(participant); match.setStatus("ACTIVE"); matchMapper.updateById(match);
        return response(matchId, userId, "已接受好友挑战");
    }

    @Override
    @Transactional
    public void reject(Long userId, Long matchId) {
        ChallengeMatch match = requireMatch(matchId); ChallengeParticipant participant = requireParticipant(userId, matchId);
        if (!userId.equals(match.getOpponentId()) || !"INVITED".equals(participant.getStatus())) throw new BusinessException("没有可拒绝的好友邀请");
        participant.setStatus("REJECTED"); participantMapper.updateById(participant); match.setStatus("REJECTED"); matchMapper.updateById(match);
    }

    @Override
    public Map<String, Object> dashboard(Long userId) {
        List<ChallengeParticipant> mine = participantMapper.selectList(new LambdaQueryWrapper<ChallengeParticipant>()
            .eq(ChallengeParticipant::getUserId, userId).eq(ChallengeParticipant::getStatus, "COMPLETED")
            .orderByDesc(ChallengeParticipant::getCompletedAt).last("LIMIT 100"));
        int wins = 0, draws = 0, losses = 0;
        for (ChallengeParticipant me : mine) {
            ChallengeMatch match = matchMapper.selectById(me.getMatchId());
            if (match == null || !"SETTLED".equals(match.getStatus())) continue;
            ChallengeParticipant other = participants(match.getId()).stream().filter(p -> !p.getUserId().equals(userId)).findFirst().orElse(null);
            if (other == null) continue;
            int compare = Integer.compare(safe(me.getScore()), safe(other.getScore()));
            if (compare > 0) wins++; else if (compare == 0) draws++; else losses++;
        }
        ChallengeSeasonCatalog.Season season = ChallengeSeasonCatalog.current(LocalDate.now());
        Leaderboard row = leaderboardMapper.selectOne(new LambdaQueryWrapper<Leaderboard>().eq(Leaderboard::getUserId, userId)
            .eq(Leaderboard::getRankType, RANK_TYPE).eq(Leaderboard::getRankWeek, season.key()).last("LIMIT 1"));
        long points = row == null || row.getRankValue() == null ? 0 : row.getRankValue();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tier", RankTierCatalog.resolve(points));
        result.put("stats", Map.of("wins", wins, "draws", draws, "losses", losses, "total", wins + draws + losses));
        result.put("season", Map.of("name", season.start() + " 至 " + season.end(),
            "remainingText", "剩 " + Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), season.end())) + " 天结算"));
        Set<Long> ranked = new HashSet<>(), friends = new HashSet<>();
        for (ChallengeParticipant p : participantMapper.selectList(new LambdaQueryWrapper<ChallengeParticipant>().eq(ChallengeParticipant::getStatus, "COMPLETED"))) {
            ChallengeMatch match = matchMapper.selectById(p.getMatchId());
            if (match == null) continue;
            if ("FRIEND".equals(match.getMatchType())) friends.add(p.getUserId()); else ranked.add(p.getUserId());
        }
        result.put("players", Map.of("rankedPlayers", ranked.size(), "friendPlayers", friends.size()));
        return result;
    }

    @Override
    public List<Map<String, Object>> records(Long userId) {
        return participantMapper.selectList(new LambdaQueryWrapper<ChallengeParticipant>()
                .eq(ChallengeParticipant::getUserId, userId).eq(ChallengeParticipant::getStatus, "COMPLETED")
                .orderByDesc(ChallengeParticipant::getCompletedAt).last("LIMIT 20"))
            .stream().map(me -> recordMap(userId, me)).filter(Objects::nonNull).toList();
    }

    private Map<String, Object> recordMap(Long userId, ChallengeParticipant me) {
        ChallengeMatch match = matchMapper.selectById(me.getMatchId());
        if (match == null || !"SETTLED".equals(match.getStatus())) return null;
        ChallengeParticipant other = participants(match.getId()).stream().filter(p -> !p.getUserId().equals(userId)).findFirst().orElse(null);
        if (other == null) return null;
        User opponent = userMapper.selectById(other.getUserId());
        int compare = Integer.compare(safe(me.getScore()), safe(other.getScore()));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", match.getId()); map.put("challengeId", match.getId()); map.put("type", match.getMatchType());
        map.put("myScore", me.getScore()); map.put("opponentScore", other.getScore()); map.put("isWin", compare > 0);
        map.put("result", compare > 0 ? "WIN" : compare == 0 ? "DRAW" : "LOSS");
        map.put("opponentId", other.getUserId()); map.put("opponentName", opponent == null ? "已注销用户" : opponent.getNickname());
        map.put("opponentAvatar", opponent == null ? null : opponent.getAvatar());
        map.put("rewardGold", rewardAmount(match.getId(), userId, "GOLD")); map.put("rankDelta", rewardAmount(match.getId(), userId, "RANK"));
        map.put("playTime", me.getCompletedAt() == null ? "" : me.getCompletedAt().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")));
        return map;
    }

    private Map<String, Object> response(Long matchId, Long userId, String message) {
        Map<String, Object> result = new LinkedHashMap<>(status(userId, matchId));
        result.put("questions", questions(userId, matchId)); result.put("message", message); return result;
    }
    private void insertParticipant(Long matchId, Long userId, String status) {
        ChallengeParticipant p = new ChallengeParticipant(); p.setMatchId(matchId); p.setUserId(userId); p.setStatus(status);
        p.setScore(0); p.setCorrectCount(0); p.setDurationMs(0L); participantMapper.insert(p);
    }
    private ChallengeMatch requireMatch(Long id) {
        ChallengeMatch match = matchMapper.selectById(id);
        if (match == null || match.getExpiresAt().isBefore(LocalDateTime.now())) throw new BusinessException("挑战不存在或已过期"); return match;
    }
    private ChallengeParticipant requireParticipant(Long userId, Long matchId) {
        requireMatch(matchId);
        ChallengeParticipant p = participantMapper.selectOne(new LambdaQueryWrapper<ChallengeParticipant>()
            .eq(ChallengeParticipant::getMatchId, matchId).eq(ChallengeParticipant::getUserId, userId).last("LIMIT 1"));
        if (p == null) throw new BusinessException("您不是本场挑战参与者"); return p;
    }
    private ChallengeParticipant requireActiveParticipant(Long userId, Long matchId) {
        ChallengeParticipant p = requireParticipant(userId, matchId);
        if (!List.of("ACTIVE", "COMPLETED").contains(p.getStatus())) throw new BusinessException("请先接受好友挑战"); return p;
    }
    private List<ChallengeParticipant> participants(Long matchId) {
        return participantMapper.selectList(new LambdaQueryWrapper<ChallengeParticipant>().eq(ChallengeParticipant::getMatchId, matchId)
            .ne(ChallengeParticipant::getStatus, "REJECTED").orderByAsc(ChallengeParticipant::getId));
    }
    private boolean isAcceptedFriend(Long userId, Long friendId) {
        Long count = friendMapper.selectCount(new LambdaQueryWrapper<Friend>().eq(Friend::getStatus, 1).and(w -> w
            .eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId).or()
            .eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId)));
        return count != null && count > 0;
    }
    private Map<String, Object> opponentMap(Long userId) {
        User user = userMapper.selectById(userId); if (user == null) return Map.of("id", userId, "nickname", "已注销用户");
        Map<String, Object> map = new LinkedHashMap<>(); map.put("id", userId); map.put("nickname", user.getNickname());
        map.put("avatar", user.getAvatar()); map.put("level", user.getLevel()); return map;
    }
    private String normalizeType(String raw) {
        String type = raw == null ? "RANKED" : raw.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("FRIEND", "RANKED", "TIMED", "COMPREHENSIVE").contains(type)) throw new BusinessException("不支持的挑战类型");
        return type;
    }
    private int safe(Integer value) { return value == null ? 0 : value; }
}
