package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.kidslearn.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true) @TableName("challenge_match")
public class ChallengeMatch extends BaseEntity {
    private String matchType; private Long creatorId; private Long opponentId; private String status;
    private String seasonKey; private String ruleSnapshot; private LocalDateTime expiresAt; private LocalDateTime settledAt;
    @Version private Integer version;
}
