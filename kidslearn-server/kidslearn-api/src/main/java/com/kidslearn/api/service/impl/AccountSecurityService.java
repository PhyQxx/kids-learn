package com.kidslearn.api.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.ParentProfile;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.ParentProfileMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.common.constants.RedisConstants;
import com.kidslearn.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountSecurityService {

    private final UserMapper userMapper;
    private final ParentProfileMapper parentProfileMapper;
    private final PasswordHashService passwordHashService;
    private final ParentPinService parentPinService;
    private final SmsVerificationService smsVerificationService;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword, String currentToken) {
        User user = requireActiveUser(userId);
        if (oldPassword == null || !passwordHashService.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        validatePassword(newPassword);
        if (passwordHashService.matches(newPassword, user.getPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }
        user.setPassword(passwordHashService.hash(newPassword));
        userMapper.updateById(user);
        revokeOtherAccessTokens(userId, currentToken);
        redisTemplate.delete(refreshTokenKey(userId));
    }

    @Transactional
    public void changePhone(Long userId, String password, String newPhone, String code) {
        User user = requireActiveUser(userId);
        if (password == null || !passwordHashService.matches(password, user.getPassword())) {
            throw new BusinessException("账号密码错误");
        }
        String phone = SmsVerificationService.normalizePhone(newPhone);
        ParentProfile occupied = parentProfileMapper.selectOne(
            new LambdaQueryWrapper<ParentProfile>().eq(ParentProfile::getPhone, phone).last("LIMIT 1")
        );
        if (occupied != null && !userId.equals(occupied.getUserId())) {
            throw new BusinessException("该手机号已绑定其他账号");
        }
        smsVerificationService.verifyAndConsume(phone, SmsVerificationService.Purpose.PHONE_CHANGE, code);
        ParentProfile profile = parentProfileMapper.selectOne(
            new LambdaQueryWrapper<ParentProfile>().eq(ParentProfile::getUserId, userId).last("LIMIT 1")
        );
        if (profile == null) {
            profile = new ParentProfile();
            profile.setUserId(userId);
            profile.setPhone(phone);
            parentProfileMapper.insert(profile);
        } else {
            profile.setPhone(phone);
            parentProfileMapper.updateById(profile);
        }
    }

    public List<Map<String, Object>> listDevices(Long userId, String currentToken) {
        var tokens = redisTemplate.opsForSet().members(accessTokenKey(userId));
        if (tokens == null) return List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        for (String token : tokens) {
            result.add(Map.of(
                "deviceId", deviceId(token),
                "name", token.equals(currentToken) ? "当前设备" : "已登录设备",
                "current", token.equals(currentToken)
            ));
        }
        result.sort(Comparator.comparing(item -> !Boolean.TRUE.equals(item.get("current"))));
        return result;
    }

    public void revokeDevice(Long userId, String deviceId, String currentToken) {
        var tokens = redisTemplate.opsForSet().members(accessTokenKey(userId));
        if (tokens == null) throw new BusinessException("设备不存在或已退出");
        String target = tokens.stream().filter(token -> deviceId(token).equals(deviceId)).findFirst()
            .orElseThrow(() -> new BusinessException("设备不存在或已退出"));
        if (target.equals(currentToken)) throw new BusinessException("请使用退出登录退出当前设备");
        redisTemplate.opsForSet().remove(accessTokenKey(userId), target);
        // Legacy refresh tokens are not device-addressable; revoke them all so the
        // removed device cannot silently obtain a new access token.
        redisTemplate.delete(refreshTokenKey(userId));
    }

    @Transactional
    public void deactivateAccount(Long userId, String password, String parentPin) {
        User user = requireActiveUser(userId);
        if (password == null || !passwordHashService.matches(password, user.getPassword())) {
            throw new BusinessException("账号密码错误");
        }
        parentPinService.verify(userId, parentPin);
        user.setStatus(0);
        userMapper.updateById(user);
        redisTemplate.delete(accessTokenKey(userId));
        redisTemplate.delete(refreshTokenKey(userId));
    }

    private User requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException("账号不可用");
        }
        return user;
    }

    private void revokeOtherAccessTokens(Long userId, String currentToken) {
        var tokens = redisTemplate.opsForSet().members(accessTokenKey(userId));
        if (tokens == null) return;
        tokens.stream().filter(token -> !token.equals(currentToken))
            .forEach(token -> redisTemplate.opsForSet().remove(accessTokenKey(userId), token));
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 50) {
            throw new BusinessException("新密码长度必须为6-50位");
        }
    }

    private static String deviceId(String token) { return DigestUtil.sha256Hex(token).substring(0, 16); }
    private static String accessTokenKey(Long userId) { return RedisConstants.USER_TOKEN + userId; }
    private static String refreshTokenKey(Long userId) { return RedisConstants.USER_TOKEN + "refresh:" + userId; }
}
