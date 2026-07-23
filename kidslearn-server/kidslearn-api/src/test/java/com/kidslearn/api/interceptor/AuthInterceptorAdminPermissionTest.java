package com.kidslearn.api.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidslearn.api.service.impl.AdminPermissionService;
import com.kidslearn.common.constants.RedisConstants;
import com.kidslearn.common.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthInterceptorAdminPermissionTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final AdminPermissionService permissionService = mock(AdminPermissionService.class);
    private final AuthInterceptor interceptor = new AuthInterceptor(redisTemplate, new ObjectMapper(), permissionService);

    @Test
    void rejectsAdminRequestWhenRolePermissionDoesNotAllowAction() throws Exception {
        String token = JwtUtil.generateToken(7L, "ADMIN", 60);
        ValueOperations<String, String> valueOperations = mockValueOperations();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        allowToken(token);
        when(permissionService.hasPermission(7L, "DELETE", "/api/v1/admin/question/9")).thenReturn(false);

        MockHttpServletRequest request = request("DELETE", "/api/v1/admin/question/9", token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(403, response.getStatus());
    }

    @Test
    void allowsAdminRequestWhenRolePermissionAllowsAction() throws Exception {
        String token = JwtUtil.generateToken(7L, "ADMIN", 60);
        ValueOperations<String, String> valueOperations = mockValueOperations();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        allowToken(token);
        when(permissionService.hasPermission(7L, "GET", "/api/v1/admin/question/list")).thenReturn(true);

        MockHttpServletRequest request = request("GET", "/api/v1/admin/question/list", token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    private MockHttpServletRequest request(String method, String uri, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> mockValueOperations() {
        return mock(ValueOperations.class);
    }

    @SuppressWarnings("unchecked")
    private void allowToken(String token) {
        SetOperations<String, String> setOperations = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember(RedisConstants.USER_TOKEN + 7L, token)).thenReturn(true);
    }
}
