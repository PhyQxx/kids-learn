package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kidslearn.api.entity.ChallengeSeason;
import com.kidslearn.api.mapper.ChallengeSeasonMapper;
import com.kidslearn.api.service.ChallengeSeasonService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ChallengeSeasonServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 23);

    @Test
    void usesDatabaseConfigWhenSeasonCoversToday() {
        ChallengeSeasonMapper mapper = mock(ChallengeSeasonMapper.class);
        ChallengeSeason row = new ChallengeSeason();
        row.setSeasonKey("S20260720");
        row.setSeasonName("2026 夏季排位赛");
        row.setStartDate(LocalDate.of(2026, 7, 20));
        row.setEndDate(LocalDate.of(2026, 8, 16));
        row.setStatus("ACTIVE");
        when(mapper.selectOne(any())).thenReturn(row);

        ChallengeSeasonService service = new ChallengeSeasonServiceImpl(mapper);
        ChallengeSeasonService.Season season = service.current(TODAY);

        assertEquals("S20260720", season.key());
        assertEquals("2026 夏季排位赛", season.name());
        assertEquals(LocalDate.of(2026, 7, 20), season.start());
        assertEquals(LocalDate.of(2026, 8, 16), season.end());
    }

    @Test
    void fallsBackToAlgorithmWhenNoConfiguredSeason() {
        ChallengeSeasonMapper mapper = mock(ChallengeSeasonMapper.class);
        when(mapper.selectOne(any())).thenReturn(null); // DB 无配置/漏配

        ChallengeSeasonService service = new ChallengeSeasonServiceImpl(mapper);
        ChallengeSeasonService.Season season = service.current(TODAY);

        // 回退算法：今天 2026-07-23 应落在算法赛季 S20260720
        assertEquals("S20260720", season.key());
        assertEquals(LocalDate.of(2026, 7, 20), season.start());
        assertEquals(LocalDate.of(2026, 8, 16), season.end());
        assertEquals("2026-07-20 至 2026-08-16", season.name());
    }

    @Test
    void nullDateFallsBackToNow() {
        ChallengeSeasonMapper mapper = mock(ChallengeSeasonMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);

        ChallengeSeasonService service = new ChallengeSeasonServiceImpl(mapper);
        // current(null) 不应抛异常，且应回退算法得到一个非空赛季
        ChallengeSeasonService.Season season = service.current((LocalDate) null);
        assertEquals(service.current().key(), season.key());
    }

    @Test
    void missingSeasonNameFallsBackToDateRange() {
        ChallengeSeasonMapper mapper = mock(ChallengeSeasonMapper.class);
        ChallengeSeason row = new ChallengeSeason();
        row.setSeasonKey("S20260720");
        row.setSeasonName(null); // DB 未填展示名
        row.setStartDate(LocalDate.of(2026, 7, 20));
        row.setEndDate(LocalDate.of(2026, 8, 16));
        row.setStatus("ACTIVE");
        when(mapper.selectOne(any())).thenReturn(row);

        ChallengeSeasonService service = new ChallengeSeasonServiceImpl(mapper);
        ChallengeSeasonService.Season season = service.current(TODAY);

        // name 为空时自动用 "start 至 end" 兜底
        assertEquals("2026-07-20 至 2026-08-16", season.name());
    }
}
