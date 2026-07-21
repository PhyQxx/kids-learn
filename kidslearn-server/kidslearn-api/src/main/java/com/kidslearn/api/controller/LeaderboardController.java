package com.kidslearn.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.DailyStats;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.DailyStatsMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.api.service.ChallengeService;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "获取排行榜")
    @GetMapping("/{type}")
    public R<List<Map<String, Object>>> getLeaderboard(
            HttpServletRequest request,
            @PathVariable String type) {
        Long currentUserId = (Long) request.getAttribute("userId");
        if ("challenge".equals(type) || "ranked".equals(type)) {
            return R.ok(challengeService.getChallengeRanking(currentUserId));
        }

        boolean isWeekly = "weekly".equals(type);
        LocalDate weekStart = isWeekly ? LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1) : null;

        // 批量查询本周所有用户的统计数据（避免N+1）
        Map<Long, Integer> weeklyExpMap = new HashMap<>();
        if (isWeekly) {
            List<DailyStats> allWeekStats = dailyStatsMapper.selectList(
                new LambdaQueryWrapper<DailyStats>()
                    .ge(DailyStats::getStatDate, weekStart)
            );
            // 按userId分组求和
            for (DailyStats ds : allWeekStats) {
                weeklyExpMap.merge(ds.getUserId(), ds.getEarnedExp() != null ? ds.getEarnedExp() : 0, Integer::sum);
            }
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
}
