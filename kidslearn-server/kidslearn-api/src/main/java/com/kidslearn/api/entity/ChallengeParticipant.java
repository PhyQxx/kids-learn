package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true) @TableName("challenge_participant")
public class ChallengeParticipant extends BaseEntity {
    private Long matchId; private Long userId; private String status; private Integer score; private Integer correctCount;
    private Long durationMs; private LocalDateTime completedAt;
}
