package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidslearn.api.entity.CourseLevel;
import com.kidslearn.api.entity.ContentAudit;
import com.kidslearn.api.entity.Question;
import com.kidslearn.api.entity.QuestionOption;
import com.kidslearn.api.entity.Subject;
import com.kidslearn.api.mapper.CourseLevelMapper;
import com.kidslearn.api.mapper.ContentAuditMapper;
import com.kidslearn.api.mapper.QuestionMapper;
import com.kidslearn.api.mapper.QuestionOptionMapper;
import com.kidslearn.api.mapper.SubjectMapper;
import com.kidslearn.api.service.AiService;
import com.kidslearn.api.service.impl.AdminOperationLogService;
import com.kidslearn.common.result.PageResult;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台-内容审核")
@RestController
@RequestMapping("/api/v1/admin/content-audit")
@RequiredArgsConstructor
public class AdminContentAuditController {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 2;
    public static final int STATUS_REJECTED = 3;

    private final ContentAuditMapper contentAuditMapper;
    private final AdminOperationLogService adminOperationLogService;
    private final SubjectMapper subjectMapper;
    private final CourseLevelMapper courseLevelMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final AiService aiService;

    @Operation(summary = "内容审核列表")
    @GetMapping("/list")
    public R<PageResult<ContentAudit>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String targetType) {
        LambdaQueryWrapper<ContentAudit> wrapper = new LambdaQueryWrapper<ContentAudit>()
            .eq(status != null, ContentAudit::getStatus, status)
            .eq(targetType != null && !targetType.isBlank(), ContentAudit::getTargetType, targetType)
            .orderByDesc(ContentAudit::getSubmitTime)
            .orderByDesc(ContentAudit::getId);
        Page<ContentAudit> p = contentAuditMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @Operation(summary = "内容审核详情")
    @GetMapping("/{id}/detail")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        ContentAudit audit = contentAuditMapper.selectById(id);
        if (audit == null) {
            return R.fail("审核记录不存在");
        }
        List<ContentAudit> history = contentAuditMapper.selectList(
            new LambdaQueryWrapper<ContentAudit>()
                .eq(ContentAudit::getTargetType, audit.getTargetType())
                .eq(ContentAudit::getTargetId, audit.getTargetId())
                .ne(ContentAudit::getId, id)
                .orderByDesc(ContentAudit::getSubmitTime)
                .last("LIMIT 10")
        );
        Map<String, Object> result = new HashMap<>();
        result.put("audit", audit);
        result.put("currentContent", buildTargetContent(audit));
        result.put("history", history);
        return R.ok(result);
    }

    @Operation(summary = "提交内容审核")
    @PostMapping("/submit")
    public R<Void> submit(@RequestBody ContentAudit audit) {
        audit.setStatus(STATUS_PENDING);
        audit.setSubmitTime(LocalDateTime.now());
        contentAuditMapper.insert(audit);
        adminOperationLogService.write(
            "content-audit",
            "submit",
            audit.getTargetType(),
            audit.getTargetId(),
            "submit audit"
        );
        return R.ok();
    }

    @Operation(summary = "审核内容")
    @PostMapping("/{id}/review")
    public R<Void> review(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String comment) {
        if (status == null || (status != STATUS_APPROVED && status != STATUS_REJECTED)) {
            return R.fail("审核状态无效");
        }

        ContentAudit audit = contentAuditMapper.selectById(id);
        if (audit == null) {
            return R.fail("审核记录不存在");
        }

        audit.setStatus(status);
        audit.setReviewComment(comment);
        audit.setReviewTime(LocalDateTime.now());
        contentAuditMapper.updateById(audit);
        adminOperationLogService.write(
            "content-audit",
            "review",
            audit.getTargetType(),
            audit.getTargetId(),
            "audit " + id + " status " + status
        );
        return R.ok();
    }

    @Operation(summary = "撤销内容审核")
    @PostMapping("/{id}/undo")
    public R<Void> undoReview(@PathVariable Long id) {
        ContentAudit audit = contentAuditMapper.selectById(id);
        if (audit == null) {
            return R.fail("审核记录不存在");
        }
        if (audit.getReviewTime() == null || audit.getReviewTime().isBefore(LocalDateTime.now().minusSeconds(5))) {
            return R.fail("撤销窗口已结束");
        }
        audit.setStatus(STATUS_PENDING);
        audit.setReviewComment(null);
        audit.setReviewerId(null);
        audit.setReviewTime(null);
        contentAuditMapper.updateById(audit);
        adminOperationLogService.write(
            "content-audit",
            "undo-review",
            audit.getTargetType(),
            audit.getTargetId(),
            "undo audit " + id
        );
        return R.ok();
    }

    @Operation(summary = "AI内容预审")
    @PostMapping("/{id}/ai-precheck")
    public R<Map<String, Object>> aiPrecheck(@PathVariable Long id) {
        ContentAudit audit = contentAuditMapper.selectById(id);
        if (audit == null) {
            return R.fail("审核记录不存在");
        }

        String content = buildTargetContent(audit);
        if (content.isBlank()) {
            return R.fail("审核对象内容不存在");
        }

        Map<String, Object> result = aiService.precheckContent(audit.getTargetType(), content);
        if (result == null || result.isEmpty()) {
            return R.fail("AI预审暂不可用");
        }
        adminOperationLogService.write(
            "content-audit",
            "ai-precheck",
            audit.getTargetType(),
            audit.getTargetId(),
            "ai precheck audit " + id
        );
        return R.ok(result);
    }

    private String buildTargetContent(ContentAudit audit) {
        String targetType = audit.getTargetType();
        if ("QUESTION".equalsIgnoreCase(targetType)) {
            return buildQuestionContent(audit.getTargetId());
        }
        if ("LEVEL".equalsIgnoreCase(targetType)) {
            CourseLevel level = courseLevelMapper.selectById(audit.getTargetId());
            if (level == null) return "";
            return "关卡名称：" + safe(level.getLevelName()) + "\n关卡描述：" + safe(level.getLevelDesc());
        }
        if ("SUBJECT".equalsIgnoreCase(targetType)) {
            Subject subject = subjectMapper.selectById(audit.getTargetId());
            if (subject == null) return "";
            return "学科名称：" + safe(subject.getSubjectName()) + "\n学科编码：" + safe(subject.getSubjectCode());
        }
        return "";
    }

    private String buildQuestionContent(Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            return "";
        }
        List<QuestionOption> options = questionOptionMapper.selectList(
            new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, questionId)
                .orderByAsc(QuestionOption::getSortOrder)
        );
        StringBuilder builder = new StringBuilder()
            .append("题目：").append(safe(question.getQuestionContent()))
            .append("\n解析：").append(safe(question.getAnalysis()))
            .append("\n选项：");
        for (int i = 0; i < options.size(); i++) {
            QuestionOption option = options.get(i);
            if (i > 0) {
                builder.append("\n");
            }
            builder.append(safe(option.getOptionLabel()))
                .append(". ")
                .append(safe(option.getOptionContent()));
            if (Integer.valueOf(1).equals(option.getIsCorrect())) {
                builder.append("（正确）");
            }
        }
        return builder.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
