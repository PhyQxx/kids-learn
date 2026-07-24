package com.kidslearn.api.dto.friend;

import lombok.Data;

@Data
public class UserSearchVO {

    /** 用户ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 等级 */
    private Integer level;

    /** 邀请码 */
    private String inviteCode;

    /** 是否已经是当前用户的好友 */
    private Boolean isFriend;
}
