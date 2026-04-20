package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("child_profile")
public class ChildProfile extends BaseEntity {
    private Long userId;
    private LocalDate birthDate;
    private Integer gradeLevel;
    private Integer gender;
    private String schoolName;
    private String className;
    private Integer learnAgeGroup;
    private Long petId;
}
