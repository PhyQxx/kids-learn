package com.kidslearn.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidslearn.api.entity.DailyStats;
import com.kidslearn.api.entity.Friend;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.DailyStatsMapper;
import com.kidslearn.api.mapper.FriendMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.api.service.ChallengeService;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

@Tag(name = "排行榜接口")
@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final UserMapper userMapper;
    private final DailyStatsMapper dailyStatsMapper;
    private final ChallengeService challengeService;
    private final FriendMapper friendMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Operation(summary = "获取排行榜")
    @GetMapping("/{type}")
    public R<List<Map<String, Object>>> getLeaderboard(
            HttpServletRequest request,
            @PathVariable String type) {
        Long currentUserId = (Long) request.getAttribute("userId");
        if ("challenge".equals(type) || "ranked".equals(type)) {
            return R.ok(challengeService.getChallengeRanking(currentUserId));
        }

        // 好友榜：只排当前用户的好友（含自己），按本周经验排序
        if ("friend".equals(type)) {
            return R.ok(buildFriendLeaderboard(currentUserId));
        }

        boolean isWeekly = "weekly".equals(type);
        LocalDate weekStart = isWeekly ? LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1) : null;

        // 批量查询本周所有用户的统计数据（缓存 5 分钟，避免每次请求全表扫描）
        Map<Long, Integer> weeklyExpMap = new HashMap<>();
        if (isWeekly) {
            final String cacheKey = "kidslearn:leaderboard:weekly:" + weekStart;
            weeklyExpMap = loadWeeklyExpMap(weekStart, cacheKey);
        }

        // Get top 50 users by total exp
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .orderByDesc(User::getTotalExp)
                .last("LIMIT 50")
        );

        List<Map<String, Object>> result = new ArrayList<>();
        boolean containsMe = false;

        for (User u : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("nickname", u.getNickname());
            map.put("avatar", u.getAvatar());

            // 使用预查询的Map获取分数，避免N+1查询
            int score = isWeekly
                ? weeklyExpMap.getOrDefault(u.getId(), 0)
                : (u.getTotalExp() != null ? u.getTotalExp() : 0);
            map.put("score", score);

            if (u.getId().equals(currentUserId)) {
                containsMe = true;
                map.put("isMe", true);
            } else {
                map.put("isMe", false);
            }
            result.add(map);
        }

        // Sort by score desc
        result.sort((a, b) -> ((Integer) b.get("score")).compareTo((Integer) a.get("score")));
        for (int i = 0; i < result.size(); i++) {
            result.get(i).put("rank", i + 1);
        }

        // 如果用户自己不在前50名内，则手动把自己的排名追加在列表最后
        if (!containsMe) {
            User me = userMapper.selectById(currentUserId);
            if (me != null) {
                Map<String, Object> myMap = new HashMap<>();
                myMap.put("id", me.getId());
                myMap.put("nickname", me.getNickname());
                myMap.put("avatar", me.getAvatar());
                myMap.put("isMe", true);

                int myScore = isWeekly
                    ? weeklyExpMap.getOrDefault(me.getId(), 0)
                    : (me.getTotalExp() != null ? me.getTotalExp() : 0);

                long myRank;
                if (isWeekly) {
                    // 计算周排名：统计分数高于我的用户数+1
                    long higherCount = weeklyExpMap.values().stream()
                        .filter(s -> s > myScore)
                        .count();
                    myRank = higherCount + 1;
                } else {
                    myRank = userMapper.selectCount(
                        new LambdaQueryWrapper<User>()
                            .gt(User::getTotalExp, myScore)
                            .eq(User::getStatus, 1)
                    ) + 1;
                }

                myMap.put("score", myScore);
                myMap.put("rank", myRank);
                result.add(myMap);
            }
        }

        return R.ok(result);
    }

    /**
     * 好友榜：当前用户 + 已确认好友，按本周经验排序。
     */
    private List<Map<String, Object>> buildFriendLeaderboard(Long currentUserId) {
        LocalDate weekStart = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        Map<Long, Integer> weeklyExpMap = loadWeeklyExpMap(weekStart, "kidslearn:leaderboard:weekly:" + weekStart);

        // 收集候选人：自己 + 所有已确认好友
        List<Long> candidateIds = new ArrayList<>();
        candidateIds.add(currentUserId);
        List<Friend> friends = friendMapper.selectList(
            new LambdaQueryWrapper<Friend>().eq(Friend::getUserId, currentUserId).eq(Friend::getStatus, 1));
        for (Friend f : friends) {
            candidateIds.add(f.getFriendId());
        }

        List<User> users = userMapper.selectBatchIds(candidateIds);
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            if (Integer.valueOf(0).equals(u.getStatus())) continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("nickname", u.getNickname());
            map.put("avatar", u.getAvatar());
            map.put("score", weeklyExpMap.getOrDefault(u.getId(), 0));
            map.put("isMe", u.getId().equals(currentUserId));
            result.add(map);
        }
        result.sort((a, b) -> ((Integer) b.get("score")).compareTo((Integer) a.get("score")));
        for (int i = 0; i < result.size(); i++) {
            result.get(i).put("rank", i + 1);
        }
        return result;
    }

    /**
     * 加载本周各用户经验汇总（带 Redis 缓存，TTL 5 分钟）。
     * 该数据对所有用户通用，缓存可避免排行榜高 QPS 下反复全表扫描 daily_stats。
     */
    @SuppressWarnings("unchecked")
    private Map<Long, Integer> loadWeeklyExpMap(LocalDate weekStart, String cacheKey) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                Map<String, Integer> raw = objectMapper.readValue(cached, new TypeReference<Map<String, Integer>>() {});
                Map<Long, Integer> result = new HashMap<>();
                raw.forEach((k, v) -> result.put(Long.valueOf(k), v));
                return result;
            }
        } catch (Exception e) {
            // 缓存反序列化失败则回源查询
        }
        Map<Long, Integer> weeklyExpMap = new HashMap<>();
        List<DailyStats> allWeekStats = dailyStatsMapper.selectList(
            new LambdaQueryWrapper<DailyStats>().ge(DailyStats::getStatDate, weekStart)
        );
        for (DailyStats ds : allWeekStats) {
            weeklyExpMap.merge(ds.getUserId(), ds.getEarnedExp() != null ? ds.getEarnedExp() : 0, Integer::sum);
        }
        try {
            Map<String, Integer> serializable = new HashMap<>();
            weeklyExpMap.forEach((k, v) -> serializable.put(String.valueOf(k), v));
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(serializable), Duration.ofMinutes(5));
        } catch (Exception e) {
            // 缓存写入失败不影响主流程
        }
        return weeklyExpMap;
    }
}
