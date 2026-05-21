package com.kidslearn.api.dto.learn;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitAnswerDTO {
    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    private Long levelId;

    @NotBlank(message = "答案不能为空")
    private String answer;

    @Min(value = 0, message = "答题时间不能为负数")
    private Integer answerTime;
}
