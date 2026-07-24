package com.kidslearn.api.realtime;

import com.kidslearn.common.constants.RedisConstants;
import com.kidslearn.common.util.JwtUtil;
import java.net.URI;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class RealtimeHandshakeInterceptor implements HandshakeInterceptor {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            if (JwtUtil.isTokenExpired(token)) {
                return false;
            }
            Long userId = JwtUtil.getUserId(token);
            // token 以 Set 形式存储（支持多设备登录），必须用 opsForSet().isMember 校验，
            // 而非 opsForValue().get（后者取出的是 Set 序列化值，永远不等于单个 token）
            Boolean isMember = redisTemplate.opsForSet().isMember(RedisConstants.USER_TOKEN + userId, token);
            if (!Boolean.TRUE.equals(isMember)) {
                return false;
            }
            attributes.put("userId", userId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private String resolveToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        URI uri = request.getURI();
        return UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
    }
}
