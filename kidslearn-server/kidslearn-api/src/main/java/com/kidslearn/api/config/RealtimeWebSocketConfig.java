package com.kidslearn.api.config;

import com.kidslearn.api.realtime.RealtimeHandshakeInterceptor;
import com.kidslearn.api.realtime.RealtimeWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class RealtimeWebSocketConfig implements WebSocketConfigurer {

    private final RealtimeWebSocketHandler realtimeWebSocketHandler;
    private final RealtimeHandshakeInterceptor realtimeHandshakeInterceptor;

    @Value("${CORS_ALLOWED_ORIGINS:*}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeWebSocketHandler, "/ws/realtime")
                .addInterceptors(realtimeHandshakeInterceptor)
                .setAllowedOriginPatterns(allowedOrigins.split(","));
    }

    /**
     * 设置 WebSocket session 空闲超时为 120 秒。
     * 客户端需定期发 PING 心跳保持活跃；移动端断网导致的不发心跳的僵尸连接，
     * 会在超时后被容器自动关闭并触发 afterConnectionClosed → unregister，
     * 避免 sessionsByUser 残留导致家长监控误报"孩子在线"。
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxSessionIdleTimeout(120_000L);
        container.setAsyncSendTimeout(10_000L);
        return container;
    }
}
