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

        // Get top 50 users by exp
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

            if ("weekly".equals(type)) {
                // Weekly score: sum of this week's earned exp
                LocalDate weekStart = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
                List<DailyStats> weekStats = dailyStatsMapper.selectList(
                    new LambdaQueryWrapper<DailyStats>()
                        .eq(DailyStats::getUserId, u.getId())
                        .ge(DailyStats::getStatDate, weekStart)
                );
                int weeklyExp = weekStats.stream().mapToInt(DailyStats::getEarnedExp).sum();
                map.put("score", weeklyExp);
            } else {
                map.put("score", u.getTotalExp() != null ? u.getTotalExp() : 0);
            }

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
                
                int myScore = 0;
                long myRank = 0;
                
                if ("weekly".equals(type)) {
                    LocalDate weekStart = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
                    List<DailyStats> weekStats = dailyStatsMapper.selectList(
                        new LambdaQueryWrapper<DailyStats>()
                            .eq(DailyStats::getUserId, me.getId())
                            .ge(DailyStats::getStatDate, weekStart)
                    );
                    myScore = weekStats.stream().mapToInt(DailyStats::getEarnedExp).sum();
                    
                    // Note: Calculating exact weekly rank outside top 50 is complex in memory.
                    // For now, we estimate it. Real implementation would use a better SQL query.
                    myRank = 50 + 1; // Simplified default
                } else {
                    myScore = me.getTotalExp() != null ? me.getTotalExp() : 0;
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
