package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidslearn.api.entity.Subject;
import com.kidslearn.api.entity.CourseLevel;
import com.kidslearn.api.entity.Question;
import com.kidslearn.api.entity.QuestionOption;
import com.kidslearn.api.mapper.*;
import com.kidslearn.api.service.AiService;
import com.kidslearn.api.service.impl.AdminOperationLogService;
import com.kidslearn.api.service.impl.QuestionAudioService;
import com.kidslearn.common.result.PageResult;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Tag(name = "管理后台-内容管理")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminContentController {

    private final SubjectMapper subjectMapper;
    private final CourseLevelMapper courseLevelMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final QuestionAudioService questionAudioService;
    private final AdminOperationLogService adminOperationLogService;
    private final AiService aiService;

    // ==================== 学科管理 ====================

    @Operation(summary = "学科列表")
    @GetMapping("/subject/list")
    public R<PageResult<Subject>> subjectList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Subject> wrapper = new LambdaQueryWrapper<Subject>()
            .like(keyword != null && !keyword.isEmpty(), Subject::getSubjectName, keyword)
            .orderByAsc(Subject::getSortOrder);
        Page<Subject> p = subjectMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @Operation(summary = "新增/编辑学科")
    @PostMapping("/subject/save")
    public R<Void> subjectSave(@RequestBody Subject subject) {
        if (subject.getId() == null) {
            subjectMapper.insert(subject);
        } else {
            subjectMapper.updateById(subject);
        }
        return R.ok();
    }

    @Operation(summary = "删除学科")
    @DeleteMapping("/subject/{id}")
    public R<Void> subjectDelete(@PathVariable Long id) {
        subjectMapper.deleteById(id);
        adminOperationLogService.write("content", "delete", "subject", id, "delete subject");
        return R.ok();
    }

    // ==================== 关卡管理 ====================

    @Operation(summary = "关卡列表")
    @GetMapping("/level/list")
    public R<PageResult<CourseLevel>> levelList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long subjectId) {
        LambdaQueryWrapper<CourseLevel> wrapper = new LambdaQueryWrapper<CourseLevel>()
            .eq(subjectId != null, CourseLevel::getSubjectId, subjectId)
            .orderByAsc(CourseLevel::getLevelNum);
        Page<CourseLevel> p = courseLevelMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @Operation(summary = "新增/编辑关卡")
    @PostMapping("/level/save")
    public R<Void> levelSave(@RequestBody CourseLevel level) {
        if (level.getId() == null) {
            courseLevelMapper.insert(level);
        } else {
            courseLevelMapper.updateById(level);
        }
        return R.ok();
    }

    @Operation(summary = "删除关卡")
    @DeleteMapping("/level/{id}")
    public R<Void> levelDelete(@PathVariable Long id) {
        courseLevelMapper.deleteById(id);
        adminOperationLogService.write("content", "delete", "level", id, "delete level");
        return R.ok();
    }

    // ==================== 题库管理 ====================

    @Operation(summary = "题目列表")
    @GetMapping("/question/list")
    public R<PageResult<Question>> questionList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long gradeLevelId,
            @RequestParam(required = false) Integer questionType) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
            .eq(subjectId != null, Question::getSubjectId, subjectId)
            .eq(gradeLevelId != null, Question::getGradeLevelId, gradeLevelId)
            .eq(questionType != null, Question::getQuestionType, questionType)
            .orderByAsc(Question::getSortOrder);
        Page<Question> p = questionMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @Operation(summary = "新增/编辑题目")
    @PostMapping("/question/save")
    @Transactional
    public R<Void> questionSave(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") != null ? Long.valueOf(body.get("id").toString()) : null;
        Question question = new Question();
        question.setId(id);
        question.setSubjectId(Long.valueOf(body.getOrDefault("subjectId", 0).toString()));
        question.setGradeLevelId(Long.valueOf(body.getOrDefault("gradeLevelId", 0).toString()));
        question.setQuestionType(Integer.valueOf(body.getOrDefault("questionType", 1).toString()));
        question.setQuestionContent(body.getOrDefault("questionContent", "").toString());
        question.setScore(Integer.valueOf(body.getOrDefault("score", 10).toString()));
        question.setTimeLimit(Integer.valueOf(body.getOrDefault("timeLimit", 0).toString()));
        question.setAnalysis(body.getOrDefault("analysis", "").toString());
        question.setSortOrder(Integer.valueOf(body.getOrDefault("sortOrder", 0).toString()));

        if (id == null) {
            questionMapper.insert(question);
        } else {
            questionMapper.updateById(question);
            questionOptionMapper.delete(
                new LambdaQueryWrapper<QuestionOption>().eq(QuestionOption::getQuestionId, id)
            );
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) body.get("options");
        if (options != null) {
            for (Map<String, Object> opt : options) {
                QuestionOption option = new QuestionOption();
                option.setQuestionId(question.getId());
                option.setOptionLabel(opt.getOrDefault("optionLabel", "").toString());
                option.setOptionContent(opt.getOrDefault("optionContent", "").toString());
                option.setIsCorrect(Integer.valueOf(opt.getOrDefault("isCorrect", 0).toString()));
                option.setSortOrder(Integer.valueOf(opt.getOrDefault("sortOrder", 0).toString()));
                questionOptionMapper.insert(option);
            }
        }
        return R.ok();
    }

    @Operation(summary = "生成/更新题目音频")
    @PostMapping("/question/{id}/audio")
    public R<Map<String, String>> questionAudio(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        String speechText = null;
        if (body != null && body.get("speechText") != null) {
            speechText = body.get("speechText").toString();
        }
        return R.ok(questionAudioService.generateQuestionAudio(id, speechText));
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/question/{id}")
    public R<Void> questionDelete(@PathVariable Long id) {
        questionMapper.deleteById(id);
        questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>().eq(QuestionOption::getQuestionId, id));
        adminOperationLogService.write("content", "delete", "question", id, "delete question");
        return R.ok();
    }

    @Operation(summary = "AI生成题目草稿")
    @PostMapping("/question/ai-generate")
    public R<Map<String, Object>> questionAiGenerate(@RequestBody Map<String, Object> body) {
        String subjectName = safe(body.get("subjectName"));
        String gradeName = safe(body.get("gradeName"));
        Integer questionType = parseInt(body.get("questionType"), 1);
        String knowledgePoint = safe(body.get("knowledgePoint"));

        Map<String, Object> draft = aiService.generateQuestionDraft(subjectName, gradeName, questionType, knowledgePoint);
        if (draft == null || draft.isEmpty()) {
            return R.fail("AI题目生成暂不可用");
        }
        return R.ok(draft);
    }

    @Operation(summary = "AI生成题目解析")
    @PostMapping("/question/ai-analysis")
    public R<Map<String, String>> questionAiAnalysis(@RequestBody Map<String, Object> body) {
        String questionContent = safe(body.get("questionContent"));
        String correctAnswer = safe(body.get("correctAnswer"));
        String existingAnalysis = safe(body.get("existingAnalysis"));
        List<String> options = parseStringList(body.get("options"));

        String analysis = aiService.generateQuestionAnalysis(questionContent, correctAnswer, options, existingAnalysis);
        if (analysis == null || analysis.isBlank()) {
            return R.fail("AI解析生成暂不可用");
        }
        return R.ok(Map.of("analysis", analysis));
    }

    @Operation(summary = "获取题目选项")
    @GetMapping("/question/{id}/options")
    public R<List<QuestionOption>> questionOptions(@PathVariable Long id) {
        List<QuestionOption> options = questionOptionMapper.selectList(
            new LambdaQueryWrapper<QuestionOption>().eq(QuestionOption::getQuestionId, id).orderByAsc(QuestionOption::getSortOrder)
        );
        return R.ok(options);
    }

    private static String safe(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private static Integer parseInt(Object value, Integer defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static List<String> parseStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
            .filter(Objects::nonNull)
            .map(Object::toString)
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .toList();
    }
}
