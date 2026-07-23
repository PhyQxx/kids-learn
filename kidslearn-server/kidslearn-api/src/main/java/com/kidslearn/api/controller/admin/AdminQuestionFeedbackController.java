package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidslearn.api.entity.QuestionFeedback;
import com.kidslearn.api.mapper.QuestionFeedbackMapper;
import com.kidslearn.api.service.QuestionFeedbackService;
import com.kidslearn.api.service.impl.AdminOperationLogService;
import com.kidslearn.common.result.PageResult;
import com.kidslearn.common.result.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/question-feedback") @RequiredArgsConstructor
public class AdminQuestionFeedbackController {
    private final QuestionFeedbackMapper mapper; private final QuestionFeedbackService service;
    private final AdminOperationLogService auditLogService;

    @GetMapping("/list")
    public R<PageResult<QuestionFeedback>> list(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int pageSize,
                                                @RequestParam(required = false) String status) {
        var p = mapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<QuestionFeedback>()
            .eq(status != null && !status.isBlank(), QuestionFeedback::getStatus, status).orderByDesc(QuestionFeedback::getCreateTime));
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @PostMapping("/{id}/resolve")
    public R<Void> resolve(HttpServletRequest request, @PathVariable Long id, @RequestParam String status,
                           @RequestParam(required = false) String note) {
        Long adminId = (Long) request.getAttribute("userId"); service.resolve(id, adminId, status, note);
        auditLogService.write("question-feedback", "resolve", "question-feedback", id, status + ":" + (note == null ? "" : note));
        return R.ok();
    }
}
