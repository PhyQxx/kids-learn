package com.kidslearn.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wrong_topic")
public class WrongTopic extends BaseEntity {
    private Long userId;
    private Long questionId;
    private String wrongAnswer;
    private String correctAnswer;
    private Integer times;
    private LocalDateTime lastWrongTime;
    private Integer isMastered;

    // Phase 12: 智能错题本字段
    private Integer masteryLevel; // 掌握度级别: 0未掌握 1练习中 2已掌握
    private Integer continuousCorrectCount; // 连续做对次数
    private LocalDateTime lastReviewTime; // 最后复习时间
    private LocalDate nextReviewDate;
    private Integer reviewCount;
    private Integer lastReviewResult;
}
