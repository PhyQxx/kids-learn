package com.kidslearn.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 30, message = "用户名长度3-30位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度6-50位")
    private String password;

    @Size(max = 20, message = "昵称最长20位")
    private String nickname;

    private Integer userType; // 1孩子 2家长
    private Integer loginType;

    // child fields
    private String birthDate;
    private Integer gradeLevel;
    private Integer gender;
    private Integer learnAgeGroup;

    // parent fields
    @Size(max = 20, message = "姓名最长20位")
    private String realName;

    @Size(max = 20, message = "手机号最长20位")
    private String phone;

    @Size(max = 20, message = "关系最长20位")
    private String relationship;
}
