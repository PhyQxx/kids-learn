package com.kidslearn.api.service;
import java.util.Map;
public interface EntitlementService {
    enum Code { COURSE_BASIC, COURSE_PREMIUM, AI_WRONG_EXPLAIN, AI_PARENT_SUMMARY, PET_PREMIUM_DECORATION, CHALLENGE_RANKED, OFFLINE_CONTENT }
    boolean has(Long userId, Code code);
    void require(Long userId, Code code);
    void consume(Long userId, Code code, int amount);
    Map<String, Object> getUserEntitlements(Long userId);
}
