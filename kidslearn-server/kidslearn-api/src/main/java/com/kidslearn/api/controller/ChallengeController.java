package com.kidslearn.api.controller;

import com.kidslearn.api.dto.challenge.CreateChallengeDTO;
import com.kidslearn.api.dto.challenge.SubmitChallengeDTO;
import com.kidslearn.api.service.ChallengeService;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "挑战赛接口")
@RestController
@RequestMapping("/api/v1/challenge")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @Operation(summary = "挑战赛面板")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> getDashboard(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(challengeService.getDashboard(userId));
    }

    @Operation(summary = "创建挑战")
    @PostMapping("/create")
    public R<Map<String, Object>> createChallenge(
            HttpServletRequest request,
            @RequestBody(required = false) CreateChallengeDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(challengeService.createChallenge(userId, dto));
    }

    @Operation(summary = "提交挑战结果")
    @PostMapping("/submit")
    public R<Map<String, Object>> submitChallengeResult(
            HttpServletRequest request,
            @RequestBody SubmitChallengeDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(challengeService.submitChallengeResult(userId, dto));
    }

    @Operation(summary = "获取挑战记录")
    @GetMapping("/records")
    public R<List<Map<String, Object>>> getChallengeRecords(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(challengeService.getChallengeRecords(userId));
    }

    @Operation(summary = "挑战积分榜")
    @GetMapping("/ranking")
    public R<List<Map<String, Object>>> getChallengeRanking(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(challengeService.getChallengeRanking(userId));
    }
}
