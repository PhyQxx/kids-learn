package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true) @TableName("challenge_answer_record")
public class ChallengeAnswerRecord extends BaseEntity {
    private Long matchId; private Long userId; private Long snapshotId; private String answer;
    private Integer isCorrect; private Integer awardedScore; private Long durationMs;
}
