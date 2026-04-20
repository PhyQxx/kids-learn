package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_level")
public class CourseLevel extends BaseEntity {
    private Long courseId;
    private Integer levelNum;
    private String levelName;
    private String levelDesc;
    private String coverUrl;
    private Integer totalQuestions;
    private Integer passScore;
    private String starThresholds;
    private Integer expReward;
    private Integer goldReward;
    private Long stickerId;
    private Integer isUnlock;
    private String unlockCondition;
    private Integer status;
}
