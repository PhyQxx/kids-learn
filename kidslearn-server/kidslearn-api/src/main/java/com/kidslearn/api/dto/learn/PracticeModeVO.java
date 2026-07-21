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

    @Schema(description = "类型: SEQUENTIAL(顺序练习), RANDOM(随机练习), MOCK_EXAM(模拟考试), ENDLESS/TIMED(兼容旧值)", example = "SEQUENTIAL")
    private String type;

    @Schema(description = "限时模式的时间限制(秒)", example = "60")
    private Integer timeLimitSeconds;

    @Schema(description = "标签，逗号分隔", example = "HOT")
    private String tags;

    @Schema(description = "所属学科ID")
    private Long subjectId;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "该模式下题库总题数")
    private Integer questionCount;
}
