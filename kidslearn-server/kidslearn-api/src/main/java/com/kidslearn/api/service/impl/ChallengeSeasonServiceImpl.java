package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.ChallengeSeason;
import com.kidslearn.api.mapper.ChallengeSeasonMapper;
import com.kidslearn.api.service.ChallengeSeasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ChallengeSeasonServiceImpl implements ChallengeSeasonService {

    private final ChallengeSeasonMapper challengeSeasonMapper;

    @Override
    public Season current(LocalDate date) {
        LocalDate today = date == null ? LocalDate.now() : date;
        ChallengeSeason row = challengeSeasonMapper.selectOne(new LambdaQueryWrapper<ChallengeSeason>()
            .ne(ChallengeSeason::getStatus, "DRAFT")
            .le(ChallengeSeason::getStartDate, today)
            .ge(ChallengeSeason::getEndDate, today)
            .orderByDesc(ChallengeSeason::getStartDate)
            .last("LIMIT 1"));
        if (row != null) {
            String name = row.getSeasonName() != null ? row.getSeasonName() : row.getStartDate() + " 至 " + row.getEndDate();
            return new Season(row.getSeasonKey(), row.getStartDate(), row.getEndDate(), name);
        }
        // 漏配或未排期：回退原 4 周算法
        ChallengeSeasonCatalog.Season fallback = ChallengeSeasonCatalog.current(today);
        return new Season(fallback.key(), fallback.start(), fallback.end(), fallback.name());
    }

    @Override
    public Season current() {
        return current(LocalDate.now());
    }
}
