package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("subscription")
public class Subscription extends BaseEntity {
    private Long userId;
    private Integer planType;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer autoRenew;
}
