package com.kidslearn.api.dto.challenge;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitChallengeAnswerDTO {
    @NotNull(message = "题目快照ID不能为空")
    private Long snapshotId;
    private String answer;
    @Min(value = 0, message = "答题耗时不能为负数")
    private Long durationMs;
}
