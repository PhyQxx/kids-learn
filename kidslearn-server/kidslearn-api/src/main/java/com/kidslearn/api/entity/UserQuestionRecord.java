package com.kidslearn.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户逐题答题记录表 - 记录每道题的作答情况（含答对的题）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_question_record")
public class UserQuestionRecord extends BaseEntity {
    private Long userId;
    private Long sessionId;
    private Long questionId;
    private Long practiceModeId;
    /** SEQUENTIAL / RANDOM / MOCK_EXAM */
    private String source;
    private String userAnswer;
    /** 0/1 */
    private Integer isCorrect;
    /** 答题用时(毫秒) */
    private Integer answerTimeMs;
}
