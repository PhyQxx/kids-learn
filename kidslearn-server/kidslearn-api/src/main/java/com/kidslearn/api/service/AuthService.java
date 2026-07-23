package com.kidslearn.api.service;

import com.kidslearn.api.dto.auth.LoginDTO;
import com.kidslearn.api.dto.auth.RegisterDTO;
import com.kidslearn.api.dto.auth.TokenVO;

public interface AuthService {

    TokenVO login(LoginDTO dto);

    TokenVO register(RegisterDTO dto);

    TokenVO refreshToken(String refreshToken);

    void verifyPassword(Long userId, String password);

    void logout(Long userId, String currentToken);

    /**
     * 发送手机验证码
     * @param phone 手机号
     */
    void resetPassword(String phone, String verifyCode, String newPassword);
}
