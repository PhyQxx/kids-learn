package com.kidslearn.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_video")
public class CourseVideo extends BaseEntity {
    private Long courseId;
    private Long courseLevelId;
    private String title;
    private String description;
    private String coverUrl;
    private String videoUrl;
    private Integer durationSeconds;
    private Integer sortOrder;
    private Integer isFree;
    private Integer status;
}
