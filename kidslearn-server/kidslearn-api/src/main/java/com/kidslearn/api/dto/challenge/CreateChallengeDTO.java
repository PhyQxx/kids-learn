package com.kidslearn.api.dto.challenge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateChallengeDTO {
    @NotBlank(message = "挑战类型不能为空")
    private String type;

    @NotNull(message = "对手ID不能为空")
    private Long opponentId;
}
