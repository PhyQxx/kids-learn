package com.kidslearn.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.Question;
import com.kidslearn.api.entity.QuestionFeedback;
import com.kidslearn.api.mapper.QuestionFeedbackMapper;
import com.kidslearn.api.mapper.QuestionMapper;
import com.kidslearn.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service @RequiredArgsConstructor
public class QuestionFeedbackService {
    private final QuestionFeedbackMapper feedbackMapper; private final QuestionMapper questionMapper;
    private static final Set<String> TYPES = Set.of("WRONG_ANSWER", "UNCLEAR", "MEDIA", "OTHER");

    public Long submit(Long userId, Long questionId, String rawType, String content) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) throw new BusinessException("题目不存在");
        String type = rawType == null ? "OTHER" : rawType.trim().toUpperCase();
        if (!TYPES.contains(type)) throw new BusinessException("反馈类型无效");
        if (content == null || content.isBlank() || content.trim().length() > 500) throw new BusinessException("反馈内容需为1-500字");
        Long existing = feedbackMapper.selectCount(new LambdaQueryWrapper<QuestionFeedback>()
            .eq(QuestionFeedback::getUserId, userId).eq(QuestionFeedback::getQuestionId, questionId).eq(QuestionFeedback::getStatus, "PENDING"));
        if (existing != null && existing > 0) throw new BusinessException("该题反馈正在处理中，请勿重复提交");
        QuestionFeedback feedback = new QuestionFeedback(); feedback.setUserId(userId); feedback.setQuestionId(questionId);
        feedback.setFeedbackType(type); feedback.setContent(content.trim()); feedback.setStatus("PENDING"); feedbackMapper.insert(feedback);
        return feedback.getId();
    }

    public void resolve(Long id, Long handlerId, String status, String note) {
        QuestionFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) throw new BusinessException("反馈不存在");
        if (!Set.of("RESOLVED", "REJECTED").contains(status)) throw new BusinessException("处理状态无效");
        feedback.setStatus(status); feedback.setHandlerId(handlerId); feedback.setHandleNote(note);
        feedback.setHandledAt(LocalDateTime.now()); feedbackMapper.updateById(feedback);
    }
}
