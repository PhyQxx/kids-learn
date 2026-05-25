package com.kidslearn.api.dto.challenge;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitChallengeDTO {
    @NotNull(message = "挑战ID不能为空")
    private Long challengeId;

    private Long opponentId;

    @NotNull(message = "用户分数不能为空")
    @Min(value = 0, message = "分数不能为负数")
    private Integer userScore;

    @Min(value = 0, message = "分数不能为负数")
    private Integer opponentScore;
}
