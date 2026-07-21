package com.kidslearn.api.service;

import com.kidslearn.api.dto.learn.DailyTaskVO;
import com.kidslearn.api.dto.learn.LevelResultVO;
import com.kidslearn.api.dto.learn.PracticeModeVO;
import com.kidslearn.api.dto.learn.SmartReviewQuizVO;
import com.kidslearn.api.dto.learn.SubmitAnswerDTO;
import com.kidslearn.api.dto.learn.SubmitVideoProgressDTO;
import com.kidslearn.common.result.PageResult;

import java.util.List;
import java.util.Map;

public interface LearnService {

    DailyTaskVO getDailyTasks(Long userId);

    List<Map<String, Object>> getSubjects(Long userId, Long gradeLevelId);

    List<Map<String, Object>> getLevels(Long userId, Long subjectId);

    List<Map<String, Object>> getCourseVideos(Long userId, Long courseId);

    Map<String, Object> submitVideoProgress(Long userId, SubmitVideoProgressDTO dto);

    List<Map<String, Object>> getQuestions(Long userId, Long levelId);

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

    // Phase 12: 专项练习与智能错题本
    /**
     * 获取专项练习模式列表
     * @param userId 用户ID
     * @param subjectId 学科ID (可选，用于筛选)
     * @return 专项练习模式列表
     */
    List<PracticeModeVO> getPracticeModes(Long userId, Long subjectId);

    /**
     * 开始一个专项练习
     * @param userId 用户ID
     * @param practiceModeId 练习模式ID
     * @return 练习会话信息 (如会话ID, 第一个题目)
     */
    Map<String, Object> startPractice(Long userId, Long practiceModeId);

    /**
     * 提交专项练习的答案
     * @param userId 用户ID
     * @param practiceSessionId 练习会话ID
     * @param dto 提交答案DTO
     * @return 答题结果
     */
    Map<String, Object> submitPracticeAnswer(Long userId, Long practiceSessionId, SubmitAnswerDTO dto);

    /**
     * 获取智能复习组卷
     * @param userId 用户ID
     * @param subjectId 学科ID (可选，用于筛选错题)
     * @param questionCount 题目数量
     * @return 智能复习组卷信息
     */
    SmartReviewQuizVO getSmartReviewQuiz(Long userId, Long subjectId, Integer questionCount);

    /**
     * 更新错题掌握度
     * @param userId 用户ID
     * @param wrongTopicId 错题记录ID
     * @param isCorrect 本次是否答对
     * @return 更新结果
     */
    Map<String, Object> updateWrongTopicMastery(Long userId, Long wrongTopicId, boolean isCorrect);

    /**
     * 断点续做：恢复某个练习会话的题目顺序、答题记录、当前进度
     * @param userId 用户ID
     * @param sessionId 练习会话ID
     * @return 会话信息（含题目列表、用户答题记录、当前题号）
     */
    Map<String, Object> resumePractice(Long userId, Long sessionId);

    /**
     * 获取某练习模式下的进行中会话进度（供模式选择页显示"继续答题"）
     * @param userId 用户ID
     * @param practiceModeId 练习模式ID
     * @return 进度信息（含 sessionId、已做/总数、当前题号）
     */
    Map<String, Object> getPracticeProgress(Long userId, Long practiceModeId);

    /**
     * 放弃某个练习会话（重新开始时调用，把 IN_PROGRESS 置为 ABANDONED）
     * @param userId 用户ID
     * @param sessionId 练习会话ID
     * @return 操作结果
     */
    Map<String, Object> abandonPractice(Long userId, Long sessionId);

    /**
     * 完成某个练习会话（把状态置为 COMPLETED）
     * @param userId 用户ID
     * @param sessionId 练习会话ID
     * @return 操作结果
     */
    Map<String, Object> completePracticeSession(Long userId, Long sessionId);
}
