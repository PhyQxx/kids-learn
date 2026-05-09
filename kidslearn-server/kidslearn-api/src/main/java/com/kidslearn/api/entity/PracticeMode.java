package com.kidslearn.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("practice_mode")
public class PracticeMode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long subjectId;

    private String name;

    private String description;

    private String icon;

    private String type;

    private Integer timeLimitSeconds;

    private String tags;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
