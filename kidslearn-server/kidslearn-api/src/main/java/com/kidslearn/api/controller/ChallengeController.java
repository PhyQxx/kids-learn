package com.kidslearn.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.ChallengeRecord;
import com.kidslearn.api.entity.CourseLevel;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.ChallengeRecordMapper;
import com.kidslearn.api.mapper.CourseLevelMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "挑战赛接口")
@RestController
@RequestMapping("/api/v1/challenge")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeRecordMapper challengeRecordMapper;
    private final UserMapper userMapper;
    private final CourseLevelMapper courseLevelMapper;

    @Operation(summary = "创建挑战")
    @PostMapping("/create")
    public R<Map<String, Object>> createChallenge(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String type = body.getOrDefault("type", "RANDOM");

        // Pick a random level for the challenge
        List<CourseLevel> levels = courseLevelMapper.selectList(
            new LambdaQueryWrapper<CourseLevel>()
                .eq(CourseLevel::getStatus, 1)
                .eq(CourseLevel::getIsUnlock, 1)
                .last("ORDER BY RAND() LIMIT 1")
        );

        Map<String, Object> result = new HashMap<>();
        if (!levels.isEmpty()) {
            result.put("levelId", levels.get(0).getId());
            result.put("levelName", levels.get(0).getLevelName());
        }
        result.put("type", type);
        result.put("message", "挑战已创建");

        return R.ok(result);
    }

    @Operation(summary = "获取挑战记录")
    @GetMapping("/records")
    public R<List<Map<String, Object>>> getChallengeRecords(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        List<ChallengeRecord> records = challengeRecordMapper.selectList(
            new LambdaQueryWrapper<ChallengeRecord>()
                .eq(ChallengeRecord::getUserId, userId)
                .orderByDesc(ChallengeRecord::getCreateTime)
                .last("LIMIT 20")
        );

        List<Map<String, Object>> result = records.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("myScore", r.getUserScore());
            map.put("opponentScore", r.getOpponentScore());
            map.put("isWin", r.getIsWinner() != null && r.getIsWinner() == 1);
            map.put("rewardGold", r.getRewardGold());
            map.put("playTime", r.getPlayTime() != null ? r.getPlayTime().toString() : null);

            if (r.getOpponentId() != null) {
                User opponent = userMapper.selectById(r.getOpponentId());
                if (opponent != null) {
                    map.put("opponentName", opponent.getNickname());
                    map.put("opponentAvatar", opponent.getAvatar());
                }
            }
            return map;
        }).collect(Collectors.toList());

        return R.ok(result);
    }
}
