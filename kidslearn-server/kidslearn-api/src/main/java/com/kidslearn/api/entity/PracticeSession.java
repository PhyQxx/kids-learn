package com.kidslearn.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 练习会话表 - 支持顺序练习断点续做
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("practice_session")
public class PracticeSession extends BaseEntity {
    private Long userId;
    private Long practiceModeId;
    private Long subjectId;
    private Long gradeLevelId;
    /** 题目ID顺序快照，逗号分隔 */
    private String questionIds;
    private Integer totalQuestions;
    /** 当前做到第几题(0-based) */
    private Integer currentIndex;
    private Integer correctCount;
    private Integer wrongCount;
    /** IN_PROGRESS / COMPLETED / ABANDONED */
    private String status;
    private LocalDateTime lastActiveTime;
}
