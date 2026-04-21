package com.kidslearn.api.service;

import com.kidslearn.api.dto.auth.LoginDTO;
import com.kidslearn.api.dto.auth.RegisterDTO;
import com.kidslearn.api.dto.auth.TokenVO;

public interface AuthService {

    TokenVO login(LoginDTO dto);

    TokenVO register(RegisterDTO dto);

    TokenVO refreshToken(String refreshToken);

    void logout(Long userId);
}
