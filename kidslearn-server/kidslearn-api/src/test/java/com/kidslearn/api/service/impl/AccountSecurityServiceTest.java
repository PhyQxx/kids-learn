package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.ParentProfileMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.common.constants.RedisConstants;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class AccountSecurityServiceTest {

    @Test
    void passwordChangeKeepsCurrentAccessAndRevokesOtherSessions() {
        UserMapper users = mock(UserMapper.class);
        PasswordHashService hashes = new PasswordHashService();
        User user = new User();
        user.setId(7L);
        user.setStatus(1);
        user.setPassword(hashes.hash("old-secret"));
        when(users.selectById(7L)).thenReturn(user);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> sets = mock(SetOperations.class);
        when(redis.opsForSet()).thenReturn(sets);
        String key = RedisConstants.USER_TOKEN + 7L;
        when(sets.members(key)).thenReturn(Set.of("current", "other"));
        AccountSecurityService service = new AccountSecurityService(
            users, mock(ParentProfileMapper.class), hashes, mock(ParentPinService.class),
            mock(SmsVerificationService.class), redis);

        service.changePassword(7L, "old-secret", "new-secret", "current");

        assertTrue(hashes.matches("new-secret", user.getPassword()));
        verify(sets).remove(key, "other");
        verify(redis).delete(RedisConstants.USER_TOKEN + "refresh:7");
    }
}
