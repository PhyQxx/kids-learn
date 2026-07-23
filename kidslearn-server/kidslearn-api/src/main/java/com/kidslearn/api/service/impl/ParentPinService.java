package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.ParentProfile;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.ParentProfileMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.common.exception.BusinessException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account-scoped parent PIN setup and verification with brute-force locking. */
@Service
@RequiredArgsConstructor
public class ParentPinService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 30;
    private static final String ATTEMPT_PREFIX = "security:parent-pin:attempts:";
    private static final String LOCK_PREFIX = "security:parent-pin:locked:";

    private final ParentProfileMapper parentProfileMapper;
    private final UserMapper userMapper;
    private final PasswordHashService passwordHashService;
    private final StringRedisTemplate redisTemplate;

    public Map<String, Object> status(Long userId) {
        ParentProfile profile = findProfile(userId);
        boolean configured = profile != null
            && profile.getParentPinHash() != null
            && !profile.getParentPinHash().isBlank();
        return Map.of("configured", configured, "pinLength", 6);
    }

    @Transactional
    public void setup(Long userId, String accountPassword, String newPin) {
        validatePin(newPin);
        User user = userMapper.selectById(userId);
        if (user == null || accountPassword == null || accountPassword.isBlank()
                || !passwordHashService.matches(accountPassword, user.getPassword())) {
            throw new BusinessException("账号密码错误");
        }
        ParentProfile profile = findProfile(userId);
        if (profile == null) {
            profile = new ParentProfile();
            profile.setUserId(userId);
            profile.setParentPinHash(passwordHashService.hash(newPin));
            parentProfileMapper.insert(profile);
        } else {
            if (profile.getParentPinHash() != null && !profile.getParentPinHash().isBlank()) {
                throw new BusinessException("家长PIN已设置，请使用修改PIN功能");
            }
            profile.setParentPinHash(passwordHashService.hash(newPin));
            parentProfileMapper.updateById(profile);
        }
        clearFailures(userId);
    }

    @Transactional
    public void change(Long userId, String currentPin, String newPin) {
        verify(userId, currentPin);
        validatePin(newPin);
        ParentProfile profile = requireConfiguredProfile(userId);
        profile.setParentPinHash(passwordHashService.hash(newPin));
        parentProfileMapper.updateById(profile);
        clearFailures(userId);
    }

    public void verify(Long userId, String pin) {
        validatePin(pin);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_PREFIX + userId))) {
            throw new BusinessException("PIN尝试次数过多，请30分钟后再试");
        }
        ParentProfile profile = requireConfiguredProfile(userId);
        if (!passwordHashService.matches(pin, profile.getParentPinHash())) {
            recordFailure(userId);
            throw new BusinessException("家长PIN错误");
        }
        clearFailures(userId);
    }

    private ParentProfile findProfile(Long userId) {
        return parentProfileMapper.selectOne(
            new LambdaQueryWrapper<ParentProfile>()
                .eq(ParentProfile::getUserId, userId)
                .last("LIMIT 1")
        );
    }

    private ParentProfile requireConfiguredProfile(Long userId) {
        ParentProfile profile = findProfile(userId);
        if (profile == null || profile.getParentPinHash() == null || profile.getParentPinHash().isBlank()) {
            throw new BusinessException("请先设置家长PIN");
        }
        return profile;
    }

    private void recordFailure(Long userId) {
        String attemptsKey = ATTEMPT_PREFIX + userId;
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts == null || attempts == 1L) {
            redisTemplate.expire(attemptsKey, LOCK_MINUTES, TimeUnit.MINUTES);
        }
        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            redisTemplate.opsForValue().set(LOCK_PREFIX + userId, "1", LOCK_MINUTES, TimeUnit.MINUTES);
            redisTemplate.delete(attemptsKey);
        }
    }

    private void clearFailures(Long userId) {
        redisTemplate.delete(ATTEMPT_PREFIX + userId);
        redisTemplate.delete(LOCK_PREFIX + userId);
    }

    private static void validatePin(String pin) {
        if (pin == null || !pin.matches("^\\d{6}$")) {
            throw new BusinessException("家长PIN必须是6位数字");
        }
    }
}
