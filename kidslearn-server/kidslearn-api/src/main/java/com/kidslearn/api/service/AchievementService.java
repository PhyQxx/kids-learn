package com.kidslearn.api.service;

import java.util.List;
import java.util.Map;

public interface AchievementService {

    List<Map<String, Object>> getAchievements(Long userId, Integer type);

    Map<String, Object> getMyProgress(Long userId);

    Map<String, Object> receiveReward(Long userId, Long achievementId);

    List<Map<String, Object>> getStickers(Long userId, Long seriesId);

    List<Map<String, Object>> getTitles(Long userId);

    Map<String, Object> activateTitle(Long userId, Long titleId);
}
