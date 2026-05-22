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

/**
 * 用户认证拦截器
 */
@Component
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("认证失败: Token缺失或格式错误, path: {}", request.getRequestURI());
            sendError(response, R.unauthorized(), HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        token = token.substring(7);
        try {
            if (JwtUtil.isTokenExpired(token)) {
                log.warn("认证失败: Token已过期");
                sendError(response, R.unauthorized(), HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            Long userId = JwtUtil.getUserId(token);
            String userType = JwtUtil.getUserType(token);
            
            // 校验Redis中的Token
            String cachedToken = redisTemplate.opsForValue().get(RedisConstants.USER_TOKEN + userId);
            if (cachedToken == null) {
                log.warn("认证失败: Redis中Token不存在, userId: {}", userId);
                sendError(response, R.unauthorized(), HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            if (!token.equals(cachedToken)) {
                log.warn("认证失败: Token不匹配, 可能在其他地方登录, userId: {}", userId);
                sendError(response, R.unauthorized(), HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            // 管理后台权限校验
            if (request.getRequestURI().startsWith("/api/v1/admin")) {
                if (!"ADMIN".equals(userType)) {
                    log.warn("权限不足: 非管理员尝试访问管理接口, userId: {}, userType: {}, path: {}", 
                        userId, userType, request.getRequestURI());
                    sendError(response, R.fail("权限不足"), HttpServletResponse.SC_FORBIDDEN);
                    return false;
                }
            }

            request.setAttribute("userId", userId);
            request.setAttribute("userType", userType);
            request.setAttribute("token", token);
            return true;
        } catch (Exception e) {
            log.error("认证过程发生异常: {}", e.getMessage());
            sendError(response, R.unauthorized(), HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    private void sendError(HttpServletResponse response, R<?> r, int status) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(status);
        response.getWriter().write(objectMapper.writeValueAsString(r));
    }
}
