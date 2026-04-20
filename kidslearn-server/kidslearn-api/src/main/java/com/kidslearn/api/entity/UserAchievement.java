package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_achievement")
public class UserAchievement extends BaseEntity {
    private Long userId;
    private Long achieveId;
    private Integer currentValue;
    private Integer targetValue;
    private Integer isCompleted;
    private LocalDateTime completedTime;
    private Integer isReceived;
}
