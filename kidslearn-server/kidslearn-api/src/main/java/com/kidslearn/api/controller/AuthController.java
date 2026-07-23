package com.kidslearn.api.controller;

import com.kidslearn.api.dto.auth.LoginDTO;
import com.kidslearn.api.dto.auth.RegisterDTO;
import com.kidslearn.api.dto.auth.TokenVO;
import com.kidslearn.api.service.AuthService;
import com.kidslearn.api.service.impl.SmsVerificationService;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SmsVerificationService smsVerificationService;

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

    @Operation(summary = "发送验证码")
    @PostMapping("/send-code")
    public R<Map<String, Object>> sendVerifyCode(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.isBlank()) {
            return R.fail("手机号不能为空");
        }
        // 简单校验手机号格式
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return R.fail("手机号格式不正确");
        }
        SmsVerificationService.Purpose purpose;
        try {
            purpose = SmsVerificationService.Purpose.valueOf(
                body.getOrDefault("purpose", "REGISTER").trim().toUpperCase());
        } catch (IllegalArgumentException error) {
            return R.fail("验证码用途不正确");
        }
        return R.ok(smsVerificationService.send(phone, purpose, clientIp(request)));
    }

    @Operation(summary = "发送忘记密码验证码")
    @PostMapping("/forgot-password/code")
    public R<Map<String, Object>> sendForgotPasswordCode(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        return R.ok(smsVerificationService.send(
            body.get("phone"), SmsVerificationService.Purpose.PASSWORD_RESET, clientIp(request)));
    }

    @Operation(summary = "重置忘记的密码")
    @PostMapping("/forgot-password/reset")
    public R<Void> resetForgottenPassword(@RequestBody Map<String, String> body) {
        authService.resetPassword(body.get("phone"), body.get("code"), body.get("newPassword"));
        return R.ok();
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh-token")
    public R<TokenVO> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return R.fail("refreshToken不能为空");
        }
        return R.ok(authService.refreshToken(refreshToken));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String token = (String) request.getAttribute("token");
        authService.logout(userId, token);
        return R.ok();
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
