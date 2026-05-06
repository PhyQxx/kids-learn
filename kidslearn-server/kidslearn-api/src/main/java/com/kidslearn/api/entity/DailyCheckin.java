package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_checkin")
public class DailyCheckin extends BaseEntity {
    private Long userId;
    private LocalDate checkinDate;
    private Integer rewardDay;
    private Integer goldReward;
    private Integer expReward;
}
