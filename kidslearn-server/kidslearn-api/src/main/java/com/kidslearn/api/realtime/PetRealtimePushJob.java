package com.kidslearn.api.realtime;

import com.kidslearn.api.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetRealtimePushJob {

    private final RealtimeSessionRegistry sessionRegistry;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final PetService petService;

    @Scheduled(fixedRate = 60000, initialDelay = 5000)
    public void pushPetStatusForConnectedUsers() {
        for (Long userId : sessionRegistry.connectedUserIds()) {
            realtimeEventPublisher.publishPetStatus(userId, petService.getMyPet(userId));
        }
    }
}
