package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.hutool.crypto.digest.DigestUtil;
import com.kidslearn.api.dto.auth.LoginDTO;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.ChildProfileMapper;
import com.kidslearn.api.mapper.GradeLevelMapper;
import com.kidslearn.api.mapper.ParentProfileMapper;
import com.kidslearn.api.mapper.PetMapper;
import com.kidslearn.api.mapper.UserLoginLogMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.api.mapper.UserPetMapper;
import com.kidslearn.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AuthServiceImplPasswordTest {

    @Test
    void legacyMd5LoginUpgradesStoredPasswordToBcrypt() {
        UserMapper userMapper = mock(UserMapper.class);
        ChildProfileMapper childProfileMapper = mock(ChildProfileMapper.class);
        ParentProfileMapper parentProfileMapper = mock(ParentProfileMapper.class);
        GradeLevelMapper gradeLevelMapper = mock(GradeLevelMapper.class);
        UserLoginLogMapper userLoginLogMapper = mock(UserLoginLogMapper.class);
        PetMapper petMapper = mock(PetMapper.class);
        UserPetMapper userPetMapper = mock(UserPetMapper.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        User user = new User();
        user.setId(7L);
        user.setUsername("demo");
        user.setPassword(DigestUtil.md5Hex("secret123"));
        user.setUserType(1);
        user.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(user);

        AuthServiceImpl service = new AuthServiceImpl(
            userMapper,
            childProfileMapper,
            parentProfileMapper,
            gradeLevelMapper,
            userLoginLogMapper,
            petMapper,
            userPetMapper,
            redisTemplate,
            new PasswordHashService()
        );

        LoginDTO dto = new LoginDTO();
        dto.setUsername("demo");
        dto.setPassword("secret123");
        service.login(dto);

        assertTrue(user.getPassword().startsWith("{bcrypt}"));
        assertTrue(new PasswordHashService().matches("secret123", user.getPassword()));
        verify(userMapper).updateById(user);
    }

    @Test
    void verifiesCurrentUserPasswordForParentMode() {
        UserMapper userMapper = mock(UserMapper.class);
        AuthServiceImpl service = serviceWith(userMapper);
        User user = new User();
        user.setId(7L);
        user.setPassword(new PasswordHashService().hash("secret123"));
        when(userMapper.selectById(7L)).thenReturn(user);

        service.verifyPassword(7L, "secret123");

        assertThrows(BusinessException.class, () -> service.verifyPassword(7L, "bad-password"));
    }

    private static AuthServiceImpl serviceWith(UserMapper userMapper) {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        return new AuthServiceImpl(
            userMapper,
            mock(ChildProfileMapper.class),
            mock(ParentProfileMapper.class),
            mock(GradeLevelMapper.class),
            mock(UserLoginLogMapper.class),
            mock(PetMapper.class),
            mock(UserPetMapper.class),
            redisTemplate,
            new PasswordHashService()
        );
    }
}
