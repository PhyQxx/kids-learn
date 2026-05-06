package com.kidslearn.api.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
public class RealtimeSessionRegistry {

    private final ObjectMapper objectMapper;
    private final Map<Long, Set<Object>> sessionsByUser = new ConcurrentHashMap<>();
    private final Map<Object, Long> userBySession = new ConcurrentHashMap<>();

    public void register(Long userId, Object session) {
        if (userId == null || session == null) {
            return;
        }
        sessionsByUser.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        userBySession.put(session, userId);
    }

    public void unregister(Object session) {
        Long userId = userBySession.remove(session);
        if (userId == null) {
            return;
        }
        Set<Object> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId);
        }
    }

    public Set<Long> connectedUserIds() {
        return Collections.unmodifiableSet(sessionsByUser.keySet());
    }

    public void sendToUser(Long userId, RealtimeMessage message) {
        Set<Object> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (Object session : sessions) {
            send(session, message);
        }
    }

    private void send(Object session, RealtimeMessage message) {
        if (!(session instanceof WebSocketSession webSocketSession) || !webSocketSession.isOpen()) {
            return;
        }
        try {
            webSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException e) {
            unregister(session);
        }
    }
}
