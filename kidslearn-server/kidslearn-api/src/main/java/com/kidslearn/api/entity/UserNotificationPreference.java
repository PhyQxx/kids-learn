package com.kidslearn.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true) @TableName("user_notification_preference")
public class UserNotificationPreference extends BaseEntity {
    private Long userId; private String eventType; private Integer inAppEnabled; private Integer pushEnabled;
}
