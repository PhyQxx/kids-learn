package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course")
public class Course extends BaseEntity {
    private Long subjectId;
    private Long gradeLevelId;
    private String courseName;
    private String courseDesc;
    private String coverUrl;
    private Integer totalLevels;
    private Integer difficulty;
    private Integer isElite;
    private Integer sortOrder;
    private Integer status;
}
