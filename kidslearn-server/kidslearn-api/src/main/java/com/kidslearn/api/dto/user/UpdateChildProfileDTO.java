package com.kidslearn.api.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateChildProfileDTO {
    private String birthDate;
    private Integer gradeLevel;
    private Integer gender;

    @Size(max = 50, message = "学校名称最长50位")
    private String schoolName;

    @Size(max = 30, message = "班级名称最长30位")
    private String className;

    private Integer learnAgeGroup;
}
