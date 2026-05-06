package com.kidslearn.api.realtime;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealtimeEventPublisher {

    private final RealtimeSessionRegistry sessionRegistry;

    public void publishToUser(Long userId, String type, Object payload) {
        sessionRegistry.sendToUser(userId, RealtimeMessage.of(type, payload));
    }

    public void publishPetStatus(Long userId, Map<String, Object> petStatus) {
        publishToUser(userId, RealtimeMessageType.PET_STATUS_UPDATE, petStatus);
    }

    public void publishBalance(Long userId, Integer gold, Integer diamond) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("gold", gold);
        payload.put("diamond", diamond);
        publishToUser(userId, RealtimeMessageType.USER_BALANCE_UPDATE, payload);
    }
}
