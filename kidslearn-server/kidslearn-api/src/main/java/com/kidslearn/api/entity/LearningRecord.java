package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("learning_record")
public class LearningRecord extends BaseEntity {
    private Long userId;
    private Long courseLevelId;
    private Integer score;
    private Integer stars;
    private Integer answerTime;
    private Integer wrongCount;
    private Integer isPass;
    private LocalDateTime playTime;
}
