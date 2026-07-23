package com.kidslearn.api.controller;

import com.kidslearn.api.dto.learn.DailyTaskVO;
import com.kidslearn.api.dto.learn.LevelResultVO;
import com.kidslearn.api.dto.learn.PracticeModeVO;
import com.kidslearn.api.dto.learn.SmartReviewQuizVO;
import com.kidslearn.api.dto.learn.SubmitAnswerDTO;
import com.kidslearn.api.dto.learn.SubmitVideoProgressDTO;
import com.kidslearn.api.service.LearnService;
import com.kidslearn.api.service.QuestionFeedbackService;
import com.kidslearn.common.result.PageResult;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    private final QuestionFeedbackService questionFeedbackService;

    @Operation(summary = "获取今日任务")
    @GetMapping("/daily-tasks")
    public R<DailyTaskVO> getDailyTasks(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getDailyTasks(userId));
    }

    @Operation(summary = "查询当前学习访问状态")
    @GetMapping("/access-status")
    public R<Map<String, Object>> getAccessStatus(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getAccessStatus(userId));
    }

    @Operation(summary = "获取学科列表")
    @GetMapping("/subjects")
    public R<List<Map<String, Object>>> getSubjects(
            HttpServletRequest request,
            @RequestParam(required = false) Long gradeLevelId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getSubjects(userId, gradeLevelId));
    }

    @Operation(summary = "获取关卡列表")
    @GetMapping("/levels")
    public R<List<Map<String, Object>>> getLevels(
            HttpServletRequest request,
            @RequestParam Long subjectId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getLevels(userId, subjectId));
    }

    @Operation(summary = "获取视频课程列表")
    @GetMapping("/videos")
    public R<List<Map<String, Object>>> getCourseVideos(
            HttpServletRequest request,
            @RequestParam Long courseId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getCourseVideos(userId, courseId));
    }

    @Operation(summary = "上报视频学习进度")
    @PostMapping("/video-progress")
    public R<Map<String, Object>> submitVideoProgress(
            HttpServletRequest request,
            @Valid @RequestBody SubmitVideoProgressDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.submitVideoProgress(userId, dto));
    }

    @Operation(summary = "获取题目列表")
    @GetMapping("/questions")
    public R<List<Map<String, Object>>> getQuestions(
            HttpServletRequest request,
            @RequestParam Long levelId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getQuestions(userId, levelId));
    }

    @Operation(summary = "提交答案")
    @PostMapping("/submit-answer")
    public R<Map<String, Object>> submitAnswer(HttpServletRequest request, @Valid @RequestBody SubmitAnswerDTO dto) {
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

    @Operation(summary = "每日签到")
    @PostMapping("/checkin")
    public R<Map<String, Object>> checkin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.checkin(userId));
    }

    @Operation(summary = "获取签到状态")
    @GetMapping("/checkin/status")
    public R<Map<String, Object>> getCheckinStatus(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getCheckinStatus(userId));
    }

    @Operation(summary = "宠物提示技能")
    @PostMapping("/hint")
    public R<Map<String, Object>> getHint(HttpServletRequest request, @RequestParam Long questionId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getHint(userId, questionId));
    }

    @Operation(summary = "获取薄弱点推荐")
    @GetMapping("/weak-points")
    public R<List<Map<String, Object>>> getWeakPoints(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getWeakPoints(userId));
    }

    @Operation(summary = "获取自适应题目")
    @GetMapping("/adaptive-questions")
    public R<List<Map<String, Object>>> getAdaptiveQuestions(
            HttpServletRequest request,
            @RequestParam(required = false) Long subjectId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getAdaptiveQuestions(userId, subjectId));
    }

    @Operation(summary = "错题重做")
    @PostMapping("/retry-wrong")
    public R<Map<String, Object>> retryWrong(
            HttpServletRequest request,
            @RequestParam Long questionId,
            @RequestParam String answer) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.retryWrong(userId, questionId, answer));
    }

    @Operation(summary = "错题AI讲解")
    @GetMapping("/explain-wrong")
    public R<Map<String, Object>> explainWrong(
            HttpServletRequest request,
            @RequestParam Long questionId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.explainWrong(userId, questionId));
    }

    @Operation(summary = "获取新手测评题目")
    @GetMapping("/assessment")
    public R<List<Map<String, Object>>> getAssessmentQuestions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getAssessmentQuestions(userId));
    }

    // --- Phase 12: 专项练习与智能错题本 ---

    @Operation(summary = "获取专项练习模式列表")
    @GetMapping("/practice/modes")
    public R<List<PracticeModeVO>> getPracticeModes(
            HttpServletRequest request,
            @RequestParam(required = false) Long subjectId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getPracticeModes(userId, subjectId));
    }

    @Operation(summary = "开始专项练习")
    @PostMapping("/practice/start")
    public R<Map<String, Object>> startPractice(
            HttpServletRequest request,
            @RequestParam Long practiceModeId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.startPractice(userId, practiceModeId));
    }

    @Operation(summary = "提交专项练习答案")
    @PostMapping("/practice/submit")
    public R<Map<String, Object>> submitPracticeAnswer(
            HttpServletRequest request,
            @RequestParam Long practiceSessionId,
            @Valid @RequestBody SubmitAnswerDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.submitPracticeAnswer(userId, practiceSessionId, dto));
    }

    @Operation(summary = "断点续做：恢复练习会话")
    @PostMapping("/practice/resume")
    public R<Map<String, Object>> resumePractice(
            HttpServletRequest request,
            @RequestParam Long sessionId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.resumePractice(userId, sessionId));
    }

    @Operation(summary = "获取练习模式进度")
    @GetMapping("/practice/progress")
    public R<Map<String, Object>> getPracticeProgress(
            HttpServletRequest request,
            @RequestParam Long practiceModeId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getPracticeProgress(userId, practiceModeId));
    }

    @Operation(summary = "放弃练习会话")
    @PostMapping("/practice/abandon")
    public R<Map<String, Object>> abandonPractice(
            HttpServletRequest request,
            @RequestParam Long sessionId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.abandonPractice(userId, sessionId));
    }

    @Operation(summary = "完成练习会话")
    @PostMapping("/practice/complete")
    public R<Map<String, Object>> completePracticeSession(
            HttpServletRequest request,
            @RequestParam Long sessionId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.completePracticeSession(userId, sessionId));
    }

    @Operation(summary = "获取智能复习组卷")
    @GetMapping("/review/smart-quiz")
    public R<SmartReviewQuizVO> getSmartReviewQuiz(
            HttpServletRequest request,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "15") Integer questionCount) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.getSmartReviewQuiz(userId, subjectId, questionCount));
    }

    @Operation(summary = "获取今日到期复习题目")
    @GetMapping("/review/due-questions")
    public R<List<Map<String, Object>>> getDueReviewQuestions(HttpServletRequest request,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "15") Integer questionCount) {
        return R.ok(learnService.getDueReviewQuestions((Long) request.getAttribute("userId"), subjectId, questionCount));
    }

    @Operation(summary = "更新错题掌握度")
    @PostMapping("/review/mastery")
    public R<Map<String, Object>> updateWrongTopicMastery(
            HttpServletRequest request,
            @RequestParam Long wrongTopicId,
            @RequestParam boolean isCorrect) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(learnService.updateWrongTopicMastery(userId, wrongTopicId, isCorrect));
    }

    @Operation(summary = "提交题目纠错反馈")
    @PostMapping("/question-feedback")
    public R<Map<String, Object>> questionFeedback(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long questionId = body.get("questionId") instanceof Number n ? n.longValue() : null;
        if (questionId == null) return R.fail("题目ID不能为空");
        Long id = questionFeedbackService.submit((Long) request.getAttribute("userId"), questionId,
            String.valueOf(body.getOrDefault("feedbackType", "OTHER")), String.valueOf(body.getOrDefault("content", "")));
        return R.ok(Map.of("feedbackId", id, "status", "PENDING"));
    }
}
