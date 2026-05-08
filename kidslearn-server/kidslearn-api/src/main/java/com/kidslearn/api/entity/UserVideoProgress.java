package com.kidslearn.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_video_progress")
public class UserVideoProgress extends BaseEntity {
    private Long userId;
    private Long videoId;
    private Integer progressSeconds;
    private Integer durationSeconds;
    private Integer progressPercent;
    private Integer completed;
    private LocalDateTime lastWatchTime;
}
