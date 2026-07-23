package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("time_control")
public class TimeControl extends BaseEntity {
    private Long userId;
    private Integer enabled;
    private Integer dailyLimitMinutes;
    private Integer limitEnabled;
    private LocalTime allowedStartTime;
    private LocalTime allowedEndTime;
    private Integer allowedWindowEnabled;
    private Integer restReminderEnabled;
    private Integer warningBeforeMinutes;
    @Version
    private Integer version;
}
