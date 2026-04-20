package com.kidslearn.api.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidslearn.common.constants.RedisConstants;
import com.kidslearn.common.result.R;
import com.kidslearn.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            sendError(response, R.unauthorized());
            return false;
        }

        token = token.substring(7);
        try {
            if (JwtUtil.isTokenExpired(token)) {
                sendError(response, R.unauthorized());
                return false;
            }
            Long adminId = JwtUtil.getUserId(token);
            String cachedToken = redisTemplate.opsForValue().get(RedisConstants.ADMIN_TOKEN + adminId);
            if (!token.equals(cachedToken)) {
                sendError(response, R.unauthorized());
                return false;
            }
            request.setAttribute("adminId", adminId);
            return true;
        } catch (Exception e) {
            sendError(response, R.unauthorized());
            return false;
        }
    }

    private void sendError(HttpServletResponse response, R<?> r) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(200);
        response.getWriter().write(objectMapper.writeValueAsString(r));
    }
}
