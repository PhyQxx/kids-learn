package com.kidslearn.api.dto.learn;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitVideoProgressDTO {
    @NotNull(message = "视频ID不能为空")
    private Long videoId;

    @NotNull(message = "进度不能为空")
    @Min(value = 0, message = "进度不能为负数")
    private Integer progressSeconds;

    @Min(value = 0, message = "时长不能为负数")
    private Integer durationSeconds;
}
