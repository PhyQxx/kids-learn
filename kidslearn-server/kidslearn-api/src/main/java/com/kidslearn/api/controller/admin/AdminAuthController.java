package com.kidslearn.api.controller.admin;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.AdminUser;
import com.kidslearn.api.mapper.AdminUserMapper;
import com.kidslearn.common.constants.RedisConstants;
import com.kidslearn.common.result.R;
import com.kidslearn.common.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag(name = "管理员认证")
@RestController
@RequestMapping("/admin/api/v1/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminUserMapper adminUserMapper;
    private final StringRedisTemplate redisTemplate;

    @Data
    public static class LoginReq {
        private String username;
        private String password;
    }

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginReq req) {
        AdminUser admin = adminUserMapper.selectOne(
            new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, req.getUsername())
        );
        if (admin == null) {
            return R.fail("用户名或密码错误");
        }

        // support both MD5 and plain passwords
        boolean matched;
        if (admin.getPassword().startsWith("{MD5}")) {
            // MD5 password from init-data.sql
            String md5 = DigestUtil.md5Hex(req.getPassword());
            matched = admin.getPassword().equals("{MD5}" + md5);
        } else {
            // Plain MD5 hash or direct comparison
            matched = DigestUtil.md5Hex(req.getPassword()).equalsIgnoreCase(admin.getPassword());
        }

        if (!matched) {
            return R.fail("用户名或密码错误");
        }
        if (admin.getStatus() == 0) {
            return R.fail("账号已被禁用");
        }

        // update login time
        admin.setLastLoginTime(LocalDateTime.now());
        adminUserMapper.updateById(admin);

        // generate token
        String token = JwtUtil.generateToken(admin.getId(), "ADMIN", RedisConstants.TOKEN_EXPIRE);
        redisTemplate.opsForValue().set(RedisConstants.ADMIN_TOKEN + admin.getId(), token,
                RedisConstants.TOKEN_EXPIRE, TimeUnit.SECONDS);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", token);
        result.put("expiresIn", RedisConstants.TOKEN_EXPIRE);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", admin.getId());
        userInfo.put("username", admin.getUsername());
        userInfo.put("realName", admin.getRealName());
        result.put("userInfo", userInfo);

        return R.ok(result);
    }

    @Operation(summary = "管理员退出")
    @PostMapping("/logout")
    public R<Void> logout(@RequestAttribute("adminId") Long adminId) {
        redisTemplate.delete(RedisConstants.ADMIN_TOKEN + adminId);
        return R.ok();
    }
}
