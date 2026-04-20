package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("grade_level")
public class GradeLevel extends BaseEntity {
    private Integer levelCode;
    private String levelName;
    private Integer ageGroup;
    private Integer minAge;
    private Integer maxAge;
}
