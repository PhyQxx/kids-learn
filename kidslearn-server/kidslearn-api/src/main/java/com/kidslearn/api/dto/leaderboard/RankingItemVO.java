package com.kidslearn.api.dto.leaderboard;

import lombok.Data;

@Data
public class RankingItemVO {

    /** 用户ID */
    private Long id;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 分数 */
    private Integer score;

    /** 排名 */
    private Long rank;

    /** 是否是当前用户 */
    private Boolean isMe;
}
