package com.kidslearn.api.controller;

import com.kidslearn.api.dto.learn.DailyTaskVO;
import com.kidslearn.api.dto.learn.LevelResultVO;
import com.kidslearn.api.dto.learn.SubmitAnswerDTO;
import com.kidslearn.api.service.LearnService;
import com.kidslearn.common.result.PageResult;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "学习接口")
@RestController
@RequestMapping("/api/v1/learn")
@RequiredArgsConstructor
public class LearnController {

    private final LearnService learnService;

    @Operation(summary = "获取今日任务")
    @GetMapping("/daily-tasks")
    public R<DailyTaskVO> getDailyTasks(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getDailyTasks(userId));
    }

    @Operation(summary = "获取学科列表")
    @GetMapping("/subjects")
    public R<List<Map<String, Object>>> getSubjects(
            HttpServletRequest request,
            @RequestParam(required = false) Long gradeLevelId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getSubjects(userId, gradeLevelId));
    }

    @Operation(summary = "获取课程列表")
    @GetMapping("/courses")
    public R<PageResult<Map<String, Object>>> getCourses(
            HttpServletRequest request,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long gradeLevelId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getCourses(userId, subjectId, gradeLevelId, page, pageSize));
    }

    @Operation(summary = "获取关卡列表")
    @GetMapping("/levels")
    public R<List<Map<String, Object>>> getLevels(
            HttpServletRequest request,
            @RequestParam Long courseId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getLevels(userId, courseId));
    }

    @Operation(summary = "获取题目列表")
    @GetMapping("/questions")
    public R<List<Map<String, Object>>> getQuestions(@RequestParam Long levelId) {
        return R.ok(learnService.getQuestions(levelId));
    }

    @Operation(summary = "提交答案")
    @PostMapping("/submit-answer")
    public R<Map<String, Object>> submitAnswer(HttpServletRequest request, @RequestBody SubmitAnswerDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.submitAnswer(userId, dto));
    }

    @Operation(summary = "完成关卡")
    @PostMapping("/complete-level")
    public R<LevelResultVO> completeLevel(
            HttpServletRequest request,
            @RequestParam Long levelId,
            @RequestParam Integer totalScore,
            @RequestParam Integer totalTime,
            @RequestParam Integer wrongCount) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.completeLevel(userId, levelId, totalScore, totalTime, wrongCount));
    }

    @Operation(summary = "获取学习记录")
    @GetMapping("/records")
    public R<List<Map<String, Object>>> getLearningRecords(
            HttpServletRequest request,
            @RequestParam(required = false) String date) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getLearningRecords(userId, date));
    }

    @Operation(summary = "获取错题本")
    @GetMapping("/wrong-topics")
    public R<List<Map<String, Object>>> getWrongTopics(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getWrongTopics(userId));
    }
}
