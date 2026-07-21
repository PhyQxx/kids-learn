package com.kidslearn.api.dto.friend;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendRequestVO {

    /** 请求ID */
    private Long requestId;

    /** 发送者ID */
    private Long userId;

    /** 发送者昵称 */
    private String nickname;

    /** 发送者头像 */
    private String avatar;

    /** 请求时间 */
    private LocalDateTime requestTime;
}
