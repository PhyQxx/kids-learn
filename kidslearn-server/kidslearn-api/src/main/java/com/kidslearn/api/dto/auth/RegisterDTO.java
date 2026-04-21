package com.kidslearn.api.dto.auth;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String nickname;
    private Integer userType; // 1孩子 2家长
    private Integer loginType;
    // child fields
    private String birthDate;
    private Integer gradeLevel;
    private Integer gender;
    private Integer learnAgeGroup;
    // parent fields
    private String realName;
    private String phone;
    private String relationship;
}
