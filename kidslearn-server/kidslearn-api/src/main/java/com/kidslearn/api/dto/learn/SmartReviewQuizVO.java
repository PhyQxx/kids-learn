package com.kidslearn.api.dto.learn;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Data
@Schema(description = "智能复习组卷结果")
public class SmartReviewQuizVO {

    @Schema(description = "本次复习的总题数", example = "15")
    private Integer totalQuestions;
    
    @Schema(description = "预计耗时（分钟）", example = "10")
    private Integer estimatedMinutes;

    @Schema(description = "题目列表")
    private List<Long> questionIds; // 暂时只返回题目ID列表，客户端可以按需获取题目详情
}
