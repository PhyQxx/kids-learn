package com.kidslearn.api.service;

import com.kidslearn.api.dto.learn.DailyTaskVO;
import com.kidslearn.api.dto.learn.LevelResultVO;
import com.kidslearn.api.dto.learn.SubmitAnswerDTO;
import com.kidslearn.api.dto.learn.SubmitVideoProgressDTO;
import com.kidslearn.common.result.PageResult;

import java.util.List;
import java.util.Map;

public interface LearnService {

    DailyTaskVO getDailyTasks(Long userId);

    List<Map<String, Object>> getSubjects(Long userId, Long gradeLevelId);

    PageResult<Map<String, Object>> getCourses(Long userId, Long subjectId, Long gradeLevelId, Integer page, Integer pageSize);

    List<Map<String, Object>> getLevels(Long userId, Long courseId);

    List<Map<String, Object>> getCourseVideos(Long userId, Long courseId);

    Map<String, Object> submitVideoProgress(Long userId, SubmitVideoProgressDTO dto);

    List<Map<String, Object>> getQuestions(Long levelId);

    Map<String, Object> submitAnswer(Long userId, SubmitAnswerDTO dto);

    LevelResultVO completeLevel(Long userId, Long levelId, Integer totalScore, Integer totalTime, Integer wrongCount);

    List<Map<String, Object>> getLearningRecords(Long userId, String date);

    List<Map<String, Object>> getWrongTopics(Long userId);

    Map<String, Object> checkin(Long userId);

    Map<String, Object> getCheckinStatus(Long userId);

    Map<String, Object> getHint(Long userId, Long questionId);

    List<Map<String, Object>> getWeakPoints(Long userId);

    List<Map<String, Object>> getAdaptiveQuestions(Long userId, Long subjectId);

    Map<String, Object> retryWrong(Long userId, Long questionId, String answer);

    Map<String, Object> explainWrong(Long userId, Long questionId);

    List<Map<String, Object>> getAssessmentQuestions(Long userId);
}
