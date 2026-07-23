package com.kidslearn.api.service;

import com.kidslearn.api.dto.challenge.CreateChallengeDTO;
import com.kidslearn.api.dto.challenge.SubmitChallengeAnswerDTO;

import java.util.List;
import java.util.Map;

public interface AsyncChallengeService {
    Map<String, Object> createOrJoin(Long userId, CreateChallengeDTO dto);
    List<Map<String, Object>> questions(Long userId, Long matchId);
    Map<String, Object> submitAnswer(Long userId, Long matchId, SubmitChallengeAnswerDTO dto);
    Map<String, Object> finish(Long userId, Long matchId);
    Map<String, Object> status(Long userId, Long matchId);
    Map<String, Object> accept(Long userId, Long matchId);
    void reject(Long userId, Long matchId);
    Map<String, Object> dashboard(Long userId);
    List<Map<String, Object>> records(Long userId);
}
