package com.kidslearn.api.dto.auth;

import lombok.Data;

@Data
public class TokenVO {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private Object userInfo;
}
