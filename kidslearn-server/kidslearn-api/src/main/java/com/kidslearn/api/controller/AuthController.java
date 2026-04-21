package com.kidslearn.api.controller;

import com.kidslearn.api.dto.auth.LoginDTO;
import com.kidslearn.api.dto.auth.RegisterDTO;
import com.kidslearn.api.dto.auth.TokenVO;
import com.kidslearn.api.service.AuthService;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<TokenVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<TokenVO> register(@Valid @RequestBody RegisterDTO dto) {
        return R.ok(authService.register(dto));
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh-token")
    public R<TokenVO> refreshToken(@RequestParam String refreshToken) {
        return R.ok(authService.refreshToken(refreshToken));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        authService.logout(userId);
        return R.ok();
    }
}
