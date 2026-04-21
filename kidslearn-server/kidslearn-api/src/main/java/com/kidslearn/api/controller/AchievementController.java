package com.kidslearn.api.controller;

import com.kidslearn.api.service.AchievementService;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "成就接口")
@RestController
@RequestMapping("/api/v1/achievement")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @Operation(summary = "获取成就列表")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> getAchievements(
            HttpServletRequest request,
            @RequestParam(required = false) Integer type) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(achievementService.getAchievements(userId, type));
    }

    @Operation(summary = "获取我的进度")
    @GetMapping("/my-progress")
    public R<Map<String, Object>> getMyProgress(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(achievementService.getMyProgress(userId));
    }

    @Operation(summary = "领取成就奖励")
    @PostMapping("/receive-reward")
    public R<Map<String, Object>> receiveReward(HttpServletRequest request, @RequestParam Long achievementId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(achievementService.receiveReward(userId, achievementId));
    }

    @Operation(summary = "获取贴纸册")
    @GetMapping("/stickers")
    public R<List<Map<String, Object>>> getStickers(
            HttpServletRequest request,
            @RequestParam(required = false) Long seriesId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(achievementService.getStickers(userId, seriesId));
    }

    @Operation(summary = "获取称号列表")
    @GetMapping("/titles")
    public R<List<Map<String, Object>>> getTitles(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(achievementService.getTitles(userId));
    }

    @Operation(summary = "佩戴称号")
    @PostMapping("/activate-title")
    public R<Map<String, Object>> activateTitle(HttpServletRequest request, @RequestParam Long titleId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(achievementService.activateTitle(userId, titleId));
    }
}
