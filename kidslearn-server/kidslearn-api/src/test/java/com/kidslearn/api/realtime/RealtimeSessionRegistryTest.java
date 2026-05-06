package com.kidslearn.api.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RealtimeSessionRegistryTest {

    @Test
    void registersAndUnregistersUserSessions() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry(new ObjectMapper());
        Object session = new Object();

        registry.register(12L, session);

        assertEquals(Set.of(12L), registry.connectedUserIds());

        registry.unregister(session);

        assertEquals(Set.of(), registry.connectedUserIds());
    }

    @Test
    void serializesRealtimeMessageEnvelope() throws Exception {
        RealtimeMessage message = RealtimeMessage.of(
            "USER_BALANCE_UPDATE",
            Map.of("gold", 88)
        );

        String json = new ObjectMapper().writeValueAsString(message);

        assertEquals("USER_BALANCE_UPDATE", new ObjectMapper().readTree(json).get("type").asText());
        assertEquals(88, new ObjectMapper().readTree(json).get("payload").get("gold").asInt());
    }
}
