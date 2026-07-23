package com.kidslearn.api.service;

import com.kidslearn.api.entity.DomainEventOutbox;
import com.kidslearn.api.mapper.DomainEventOutboxMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class NotificationEventService {
    private final DomainEventOutboxMapper mapper;

    public void publish(String eventId, Long userId, String type, String title, String content,
                        String actionType, String actionTarget, LocalDateTime expireTime) {
        DomainEventOutbox event = new DomainEventOutbox(); event.setEventId(eventId); event.setUserId(userId);
        event.setEventType(type); event.setTitle(title); event.setContent(content); event.setActionType(actionType);
        event.setActionTarget(actionTarget); event.setExpireTime(expireTime); event.setStatus("PENDING"); event.setAttempts(0);
        mapper.insert(event);
    }
}
