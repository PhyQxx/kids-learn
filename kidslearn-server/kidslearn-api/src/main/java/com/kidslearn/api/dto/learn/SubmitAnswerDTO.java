package com.kidslearn.api.dto.learn;

import lombok.Data;

@Data
public class SubmitAnswerDTO {
    private Long levelId;
    private Long questionId;
    private String answer;
    private Integer answerTime;
}
