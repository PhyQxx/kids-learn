package com.kidslearn.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.DailyStats;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.DailyStatsMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "排行榜接口")
@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final UserMapper userMapper;
    private final DailyStatsMapper dailyStatsMapper;

    @Operation(summary = "获取排行榜")
    @GetMapping("/{type}")
    public R<List<Map<String, Object>>> getLeaderboard(
            HttpServletRequest request,
            @PathVariable String type) {
        Long currentUserId = (Long) request.getAttribute("userId");

        // Get all users with their total exp
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .orderByDesc(User::getTotalExp)
                .last("LIMIT 50")
        );

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
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
                map.put("score", u.getTotalExp());
            }

            map.put("isMe", u.getId().equals(currentUserId));
            result.add(map);
            rank++;
        }

        // Sort by score desc
        result.sort((a, b) -> ((Integer) b.get("score")).compareTo((Integer) a.get("score")));

        return R.ok(result);
    }
}
