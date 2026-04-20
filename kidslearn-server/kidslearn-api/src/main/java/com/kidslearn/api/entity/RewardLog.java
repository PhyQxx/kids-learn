package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reward_log")
public class RewardLog extends BaseEntity {
    private Long userId;
    private Integer rewardType;
    private Long rewardItemId;
    private Integer quantity;
    private String sourceType;
    private Long sourceId;
    private String description;
}
