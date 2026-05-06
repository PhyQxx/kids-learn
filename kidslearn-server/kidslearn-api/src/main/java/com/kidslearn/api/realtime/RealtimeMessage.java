package com.kidslearn.api.realtime;

import java.time.LocalDateTime;

public record RealtimeMessage(String type, Object payload, String timestamp) {

    public static RealtimeMessage of(String type, Object payload) {
        return new RealtimeMessage(type, payload, LocalDateTime.now().toString());
    }
}
