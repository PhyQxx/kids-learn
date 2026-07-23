package com.kidslearn.api.controller;

import com.kidslearn.api.dto.challenge.CreateChallengeDTO;
import com.kidslearn.api.dto.challenge.SubmitChallengeDTO;
import com.kidslearn.api.dto.challenge.SubmitChallengeAnswerDTO;
import com.kidslearn.api.service.AsyncChallengeService;
import com.kidslearn.api.service.ChallengeService;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    private final AsyncChallengeService asyncChallengeService;

    @Operation(summary = "挑战赛面板")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> getDashboard(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(asyncChallengeService.dashboard(userId));
    }

    @Operation(summary = "创建挑战")
    @PostMapping("/create")
    public R<Map<String, Object>> createChallenge(
            HttpServletRequest request,
            @Valid @RequestBody(required = false) CreateChallengeDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(asyncChallengeService.createOrJoin(userId, dto));
    }

    @Operation(summary = "提交挑战结果")
    @PostMapping("/submit")
    public R<Map<String, Object>> submitChallengeResult(
            HttpServletRequest request,
            @Valid @RequestBody SubmitChallengeDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        throw new com.kidslearn.common.exception.BusinessException("旧版客户端总分结算已停用，请升级后逐题提交");
    }

    @Operation(summary = "获取挑战题目快照")
    @GetMapping("/matches/{matchId}/questions")
    public R<List<Map<String, Object>>> questions(HttpServletRequest request, @PathVariable Long matchId) {
        return R.ok(asyncChallengeService.questions((Long) request.getAttribute("userId"), matchId));
    }

    @Operation(summary = "提交单题答案（服务端判分）")
    @PostMapping("/matches/{matchId}/answers")
    public R<Map<String, Object>> answer(HttpServletRequest request, @PathVariable Long matchId,
                                         @Valid @RequestBody SubmitChallengeAnswerDTO dto) {
        return R.ok(asyncChallengeService.submitAnswer((Long) request.getAttribute("userId"), matchId, dto));
    }

    @Operation(summary = "完成挑战")
    @PostMapping("/matches/{matchId}/finish")
    public R<Map<String, Object>> finish(HttpServletRequest request, @PathVariable Long matchId) {
        return R.ok(asyncChallengeService.finish((Long) request.getAttribute("userId"), matchId));
    }

    @Operation(summary = "挑战状态")
    @GetMapping("/matches/{matchId}")
    public R<Map<String, Object>> status(HttpServletRequest request, @PathVariable Long matchId) {
        return R.ok(asyncChallengeService.status((Long) request.getAttribute("userId"), matchId));
    }

    @Operation(summary = "接受好友挑战")
    @PostMapping("/matches/{matchId}/accept")
    public R<Map<String, Object>> accept(HttpServletRequest request, @PathVariable Long matchId) {
        return R.ok(asyncChallengeService.accept((Long) request.getAttribute("userId"), matchId));
    }

    @Operation(summary = "拒绝好友挑战")
    @PostMapping("/matches/{matchId}/reject")
    public R<Void> reject(HttpServletRequest request, @PathVariable Long matchId) {
        asyncChallengeService.reject((Long) request.getAttribute("userId"), matchId);
        return R.ok();
    }

    @Operation(summary = "获取挑战记录")
    @GetMapping("/records")
    public R<List<Map<String, Object>>> getChallengeRecords(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(asyncChallengeService.records(userId));
    }

    @Operation(summary = "挑战积分榜")
    @GetMapping("/ranking")
    public R<List<Map<String, Object>>> getChallengeRanking(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(challengeService.getChallengeRanking(userId));
    }
}
