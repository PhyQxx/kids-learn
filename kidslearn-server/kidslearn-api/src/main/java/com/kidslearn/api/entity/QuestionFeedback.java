package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper = true) @TableName("question_feedback")
public class QuestionFeedback extends BaseEntity {
    private Long userId; private Long questionId; private String feedbackType; private String content;
    private String status; private Long handlerId; private String handleNote; private LocalDateTime handledAt;
}
