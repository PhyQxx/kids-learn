package com.kidslearn.api.dto.auth;

import lombok.Data;

@Data
public class LoginDTO {
    private String username;
    private String password;
    private Integer loginType; // 1孩子 2家长
}
