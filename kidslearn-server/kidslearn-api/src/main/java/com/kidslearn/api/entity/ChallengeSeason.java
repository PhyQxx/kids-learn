package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("challenge_season")
public class ChallengeSeason extends BaseEntity {
    /** 与 leaderboard.rank_week / challenge_match.season_key 对齐的赛季键，如 S20260720 */
    private String seasonKey;
    /** 前端展示名 */
    private String seasonName;
    private LocalDate startDate;
    private LocalDate endDate;
    /** DRAFT 草稿 / ACTIVE 进行中 / SETTLED 已结算 */
    private String status;
}
