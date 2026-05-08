package com.kidslearn.api.dto.challenge;

import lombok.Data;

@Data
public class SubmitChallengeDTO {
    private Long challengeId;
    private Long opponentId;
    private Integer userScore;
    private Integer opponentScore;
}
