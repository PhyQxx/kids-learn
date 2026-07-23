package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true) @TableName("challenge_question_snapshot")
public class ChallengeQuestionSnapshot extends BaseEntity {
    private Long matchId; private Long questionId; private Integer sequenceNo; private Integer score;
    private String questionContent; private Integer questionType; private String optionsJson; private String correctAnswer;
}
