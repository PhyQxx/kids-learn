package com.kidslearn.api.dto.challenge;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateChallengeDTO {
    @NotBlank(message = "挑战类型不能为空")
    private String type;

    private Long opponentId;
}
