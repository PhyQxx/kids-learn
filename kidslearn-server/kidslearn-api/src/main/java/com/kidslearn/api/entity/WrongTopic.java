package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
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
}
