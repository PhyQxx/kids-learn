package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("achievement_tier")
public class AchievementTier extends BaseEntity {
    private Long achieveId;
    private Integer tierLevel;
    private String tierName;
    private String conditionJson;
    private String rewardJson;
}
