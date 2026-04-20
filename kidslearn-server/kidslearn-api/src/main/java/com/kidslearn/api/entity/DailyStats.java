package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_stats")
public class DailyStats extends BaseEntity {
    private Long userId;
    private LocalDate statDate;
    private Integer learnMinutes;
    private Integer completedLevels;
    private Integer earnedGold;
    private Integer earnedExp;
    private Integer loginCount;
    private Integer petFeedCount;
}
