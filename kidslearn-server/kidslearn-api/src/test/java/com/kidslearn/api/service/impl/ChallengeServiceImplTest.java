package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kidslearn.api.dto.challenge.SubmitChallengeDTO;
import com.kidslearn.api.entity.Challenge;
import com.kidslearn.api.entity.ChallengeRecord;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.ChallengeMapper;
import com.kidslearn.api.mapper.ChallengeRecordMapper;
import com.kidslearn.api.mapper.CourseLevelMapper;
import com.kidslearn.api.mapper.FriendMapper;
import com.kidslearn.api.mapper.LeaderboardMapper;
import com.kidslearn.api.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChallengeServiceImplTest {

    @Test
    void submitChallengeResultPersistsAndReturnsRankDelta() {
        ChallengeMapper challengeMapper = mock(ChallengeMapper.class);
        ChallengeRecordMapper challengeRecordMapper = mock(ChallengeRecordMapper.class);
        CourseLevelMapper courseLevelMapper = mock(CourseLevelMapper.class);
        FriendMapper friendMapper = mock(FriendMapper.class);
        LeaderboardMapper leaderboardMapper = mock(LeaderboardMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        ChallengeServiceImpl service = new ChallengeServiceImpl(
            challengeMapper,
            challengeRecordMapper,
            courseLevelMapper,
            friendMapper,
            leaderboardMapper,
            userMapper
        );

        Challenge challenge = new Challenge();
        challenge.setId(9L);
        when(challengeMapper.selectById(9L)).thenReturn(challenge);
        User user = new User();
        user.setId(1L);
        user.setGold(0);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(leaderboardMapper.selectOne(any())).thenReturn(null);

        SubmitChallengeDTO dto = new SubmitChallengeDTO();
        dto.setChallengeId(9L);
        dto.setUserScore(90);
        dto.setOpponentScore(70);

        var result = service.submitChallengeResult(1L, dto);

        ArgumentCaptor<ChallengeRecord> recordCaptor = ArgumentCaptor.forClass(ChallengeRecord.class);
        verify(challengeRecordMapper).insert(recordCaptor.capture());
        assertEquals(30, recordCaptor.getValue().getRankDelta());
        assertEquals(30, result.get("rankDelta"));
    }
}
