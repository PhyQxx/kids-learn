package com.kidslearn.api.dto.notification;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {

    /** 通知ID */
    private Long id;

    /** 类型：system/achievement/friend/challenge/learning */
    private String type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;
    private String actionType;
    private String actionTarget;
    private LocalDateTime expireTime;

    /** 是否已读 */
    private Boolean isRead;

    /** 创建时间 */
    private LocalDateTime createTime;
}
