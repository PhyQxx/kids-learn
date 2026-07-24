package com.kidslearn.api.service;

import java.time.LocalDate;

public interface ChallengeSeasonService {

    /** 排位赛季：key 对齐 leaderboard.rank_week，name 用于前端展示。 */
    record Season(String key, LocalDate start, LocalDate end, String name) {}

    /**
     * 返回指定日期所属的排位赛季。优先读数据库配置（challenge_season 表中
     * status 非 DRAFT 且区间覆盖该日期的、start_date 最大的一条）；
     * 查不到时回退原 4 周算法兜底，保证系统永不中断。
     */
    Season current(LocalDate date);

    /** 等价于 current(LocalDate.now())。 */
    Season current();
}
