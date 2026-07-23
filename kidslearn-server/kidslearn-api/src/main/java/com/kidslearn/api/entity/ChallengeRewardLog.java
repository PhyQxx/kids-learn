package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true) @TableName("challenge_reward_log")
public class ChallengeRewardLog extends BaseEntity {
    private Long matchId; private Long userId; private String rewardType; private Integer amount;
}
