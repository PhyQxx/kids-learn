package com.kidslearn.api.service;

import com.kidslearn.api.dto.challenge.CreateChallengeDTO;
import com.kidslearn.api.dto.challenge.SubmitChallengeDTO;

import java.util.List;
import java.util.Map;

public interface ChallengeService {
    Map<String, Object> createChallenge(Long userId, CreateChallengeDTO dto);

    Map<String, Object> submitChallengeResult(Long userId, SubmitChallengeDTO dto);

    Map<String, Object> getDashboard(Long userId);

    List<Map<String, Object>> getChallengeRecords(Long userId);

    List<Map<String, Object>> getChallengeRanking(Long userId);
}
