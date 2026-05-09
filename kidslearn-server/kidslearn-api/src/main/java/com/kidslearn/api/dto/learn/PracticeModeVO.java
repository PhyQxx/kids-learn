package com.kidslearn.api.dto.learn;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "专项练习模式视图对象")
public class PracticeModeVO {

    @Schema(description = "模式ID")
    private Long id;

    @Schema(description = "练习名称", example = "20以内加法")
    private String name;

    @Schema(description = "练习描述", example = "自动生成题目，练到完全掌握为止")
    private String description;

    @Schema(description = "图标或Emoji", example = "➕")
    private String icon;

    @Schema(description = "类型: ENDLESS(无尽), TIMED(限时)", example = "ENDLESS")
    private String type;

    @Schema(description = "限时模式的时间限制(秒)", example = "60")
    private Integer timeLimitSeconds;

    @Schema(description = "标签，逗号分隔", example = "HOT")
    private String tags;
}
