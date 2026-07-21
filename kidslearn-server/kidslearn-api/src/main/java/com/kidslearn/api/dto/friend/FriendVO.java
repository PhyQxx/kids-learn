package com.kidslearn.api.dto.friend;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendVO {

    /** 好友ID */
    private Long friendId;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 等级 */
    private Integer level;

    /** 添加时间 */
    private LocalDateTime addTime;
}
