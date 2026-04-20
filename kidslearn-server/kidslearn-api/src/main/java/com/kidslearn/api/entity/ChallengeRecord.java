package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("challenge_record")
public class ChallengeRecord extends BaseEntity {
    private Long challengeId;
    private Long userId;
    private Long opponentId;
    private Integer userScore;
    private Integer opponentScore;
    private Integer isWinner;
    private Integer rewardGold;
    private LocalDateTime playTime;
}
