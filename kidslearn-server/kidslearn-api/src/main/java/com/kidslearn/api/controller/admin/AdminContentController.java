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

import java.util.HashMap;
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
        // 检查是否有关联的关卡
        Long levelCount = courseLevelMapper.selectCount(
            new LambdaQueryWrapper<CourseLevel>().eq(CourseLevel::getSubjectId, id)
        );
        if (levelCount > 0) {
            return R.fail("该学科下还有 " + levelCount + " 个关卡，请先删除关卡");
        }
        subjectMapper.deleteById(id);
        adminOperationLogService.write("content", "delete", "subject", id, "delete subject");
        return R.ok();
    }

    // ==================== 关卡管理 ====================

    @Operation(summary = "关卡列表")
    @GetMapping("/level/list")
    public R<PageResult<CourseLevel>> levelList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize,
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
        String engine = null;
        if (body != null) {
            if (body.get("speechText") != null) {
                speechText = body.get("speechText").toString();
            }
            if (body.get("engine") != null) {
                engine = body.get("engine").toString();
            }
        }
        return R.ok(questionAudioService.generateQuestionAudio(id, speechText, engine));
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

    @Operation(summary = "AI生成图片")
    @PostMapping("/image/ai-generate")
    public R<Map<String, String>> aiGenerateImage(@RequestBody Map<String, Object> body) {
        String prompt = safe(body.get("prompt"));
        if (prompt.isBlank()) {
            return R.fail("请输入图片描述");
        }
        String size = safe(body.get("size"));
        String imageUrl = aiService.generateImage(prompt, size.isBlank() ? null : size);
        return R.ok(Map.of("imageUrl", imageUrl));
    }

    @Operation(summary = "AI批量评分题目难度")
    @PostMapping("/question/ai-rate-difficulty")
    public R<Map<String, Object>> aiRateDifficulty(@RequestParam(defaultValue = "50") Integer batchSize) {
        if (!aiService.isAvailable()) {
            return R.fail("AI服务不可用，请先在AI配置中设置有效的API Key");
        }

        // 查询待评分题目（difficulty 为空或0）
        List<Question> pending = questionMapper.selectList(
            new LambdaQueryWrapper<Question>()
                .and(w -> w.isNull(Question::getDifficulty).or().eq(Question::getDifficulty, 0))
                .last("LIMIT " + batchSize)
        );

        if (pending.isEmpty()) {
            return R.ok(Map.of("message", "没有待评分的题目", "rated", 0, "remaining", 0));
        }

        // 题型映射
        Map<Integer, String> typeNames = Map.of(1, "选择题", 2, "判断题", 3, "填空题", 4, "排序题", 5, "连线题");

        // 构建题目列表
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pending.size(); i++) {
            Question q = pending.get(i);
            String text = com.kidslearn.common.util.RichContentUtil.toPlainText(q.getQuestionContent());
            if (text.length() > 150) text = text.substring(0, 150);
            String typeName = typeNames.getOrDefault(q.getQuestionType(), "未知");
            sb.append(i + 1).append(". [").append(typeName).append("] ").append(text).append("\n");
        }

        String prompt = "你是一位资深的儿童教育专家，请为以下题目评估难度（1-5分）。\n\n" +
            "难度标准（针对3-12岁儿童）：\n" +
            "- 1分（很简单）：基础认知，如认字、数数、简单配对\n" +
            "- 2分（简单）：直接记忆，如拼音、简单加减、词语搭配\n" +
            "- 3分（中等）：需要理解，如阅读理解、应用题、逻辑推理入门\n" +
            "- 4分（较难）：需要分析，如多步推理、综合应用、抽象概念\n" +
            "- 5分（挑战）：需要创造，如开放性问题、复杂逻辑、跨知识点\n\n" +
            "题目列表：\n" + sb + "\n" +
            "请只返回JSON数组，格式为 [{\"index\":1,\"difficulty\":2}, ...]，index从1开始。不要其他文字。";

        try {
            String response = aiService.chatCompletion(List.of(
                Map.of("role", "user", "content", prompt)
            ), 2000, 0.3);

            // 解析JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            // 提取JSON数组
            String json = response.replaceAll("(?s).*?(\\[.*\\]).*", "$1").trim();
            List<Map<String, Object>> result = mapper.readValue(json,
                mapper.getTypeFactory().constructCollectionType(List.class, Map.class));

            int rated = 0;
            for (Map<String, Object> item : result) {
                int idx = ((Number) item.get("index")).intValue();
                int difficulty = ((Number) item.get("difficulty")).intValue();
                if (idx >= 1 && idx <= pending.size() && difficulty >= 1 && difficulty <= 5) {
                    Question q = pending.get(idx - 1);
                    q.setDifficulty(difficulty);
                    questionMapper.updateById(q);
                    rated++;
                }
            }

            // 统计剩余
            Long remaining = questionMapper.selectCount(
                new LambdaQueryWrapper<Question>()
                    .and(w -> w.isNull(Question::getDifficulty).or().eq(Question::getDifficulty, 0))
            );

            Map<String, Object> data = new HashMap<>();
            data.put("rated", rated);
            data.put("total", pending.size());
            data.put("remaining", remaining);
            return R.ok(data);
        } catch (Exception e) {
            return R.fail("AI评分失败: " + e.getMessage());
        }
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
