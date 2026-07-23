package com.kidslearn.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data @EqualsAndHashCode(callSuper = true) @TableName("domain_event_outbox")
public class DomainEventOutbox extends BaseEntity {
    private String eventId; private Long userId; private String eventType; private String title; private String content;
    private String actionType; private String actionTarget; private LocalDateTime expireTime;
    private String status; private Integer attempts; private String lastError; private LocalDateTime processedAt;
}
