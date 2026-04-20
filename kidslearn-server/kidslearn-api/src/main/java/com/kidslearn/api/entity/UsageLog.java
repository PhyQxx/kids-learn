package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("usage_log")
public class UsageLog extends BaseEntity {
    private Long userId;
    private String appName;
    private Integer duration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDate date;
}
