package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kidslearn.common.exception.BusinessException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class SmsVerificationServiceTest {

    @Test
    void normalizesPhoneAndRejectsInvalidValues() {
        assertEquals("13800000000", SmsVerificationService.normalizePhone("138 0000 0000"));
        assertThrows(BusinessException.class, () -> SmsVerificationService.normalizePhone("123"));
    }

    @Test
    void productionDoesNotPretendSuccessWithoutGatewayAndPepper() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(any(), any(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(values.increment(any())).thenReturn(1L);
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[] { "prod" });
        SmsVerificationService service = new SmsVerificationService(
            redis, environment, "dev-only-change-me", "", "", "246810", 5, 60, 10, 20, 5, 30);

        assertThrows(BusinessException.class, () ->
            service.send("13800000000", SmsVerificationService.Purpose.REGISTER, "127.0.0.1"));
    }
}
