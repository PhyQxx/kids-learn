package com.kidslearn.api.dto.challenge;

import lombok.Data;

@Data
public class CreateChallengeDTO {
    private String type;
    private Long opponentId;
}
