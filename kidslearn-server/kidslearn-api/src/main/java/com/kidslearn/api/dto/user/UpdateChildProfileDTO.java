package com.kidslearn.api.dto.user;

import lombok.Data;

@Data
public class UpdateChildProfileDTO {
    private String birthDate;
    private Integer gradeLevel;
    private Integer gender;
    private String schoolName;
    private String className;
    private Integer learnAgeGroup;
}
