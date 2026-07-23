package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kidslearn.api.entity.ParentProfile;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.ParentProfileMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class ParentPinServiceTest {

    @Test
    void existingAccountSetsPinOnlyAfterPasswordVerification() {
        ParentProfileMapper profileMapper = mock(ParentProfileMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        PasswordHashService hashes = new PasswordHashService();
        ParentPinService service = new ParentPinService(profileMapper, userMapper, hashes, redis);
        User user = new User();
        user.setPassword(hashes.hash("account-secret"));
        when(userMapper.selectById(7L)).thenReturn(user);
        ParentProfile profile = new ParentProfile();
        profile.setId(3L);
        profile.setUserId(7L);
        when(profileMapper.selectOne(any())).thenReturn(profile);

        assertThrows(BusinessException.class, () -> service.setup(7L, "wrong", "123456"));
        service.setup(7L, "account-secret", "123456");

        verify(profileMapper).updateById(profile);
    }

    @Test
    void wrongPinIsRejectedAndCorrectPinClearsFailures() {
        ParentProfileMapper profileMapper = mock(ParentProfileMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(any())).thenReturn(1L);
        PasswordHashService hashes = new PasswordHashService();
        ParentProfile profile = new ParentProfile();
        profile.setUserId(7L);
        profile.setParentPinHash(hashes.hash("123456"));
        when(profileMapper.selectOne(any())).thenReturn(profile);
        ParentPinService service = new ParentPinService(profileMapper, userMapper, hashes, redis);

        assertThrows(BusinessException.class, () -> service.verify(7L, "654321"));
        service.verify(7L, "123456");

        verify(redis).delete("security:parent-pin:attempts:7");
    }
}
