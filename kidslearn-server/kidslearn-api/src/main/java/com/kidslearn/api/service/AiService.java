package com.kidslearn.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.kidslearn.api.entity.AppConfig;
import com.kidslearn.api.mapper.AppConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final AppConfigMapper appConfigMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, String> configCache = new ConcurrentHashMap<>();
    private long cacheTime = 0;
    private static final long CACHE_TTL_MS = 60_000; // 1分钟缓存

    public AiService(AppConfigMapper appConfigMapper) {
        this.appConfigMapper = appConfigMapper;
    }

    private String getConfig(String key) {
        long now = System.currentTimeMillis();
        if (configCache.isEmpty() || now - cacheTime > CACHE_TTL_MS) {
            refreshCache();
        }
        return configCache.getOrDefault(key, "");
    }

    private synchronized void refreshCache() {
        long now = System.currentTimeMillis();
        if (!configCache.isEmpty() && now - cacheTime <= CACHE_TTL_MS) {
            return;
        }
        configCache.clear();
        List<AppConfig> configs = appConfigMapper.selectList(
            new LambdaQueryWrapper<AppConfig>().likeRight(AppConfig::getConfigKey, "ai.")
        );
        for (AppConfig c : configs) {
            configCache.put(c.getConfigKey(), c.getConfigValue());
        }
        cacheTime = now;
    }

    public void clearCache() {
        configCache.clear();
        cacheTime = 0;
    }

    private String getProvider() {
        return getConfig("ai.provider");
    }

    private String getApiKey() {
        String provider = getProvider();
        return getConfig("ai." + provider + ".api_key");
    }

    private String getBaseUrl() {
        String provider = getProvider();
        return getConfig("ai." + provider + ".base_url");
    }

    private String getModel() {
        String provider = getProvider();
        return getConfig("ai." + provider + ".model");
    }

    private int getTimeout() {
        try {
            return Integer.parseInt(getConfig("ai.timeout"));
        } catch (NumberFormatException e) {
            return 15;
        }
    }

    public boolean isAvailable() {
        String apiKey = getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    public String explainWrongTopic(String questionText, String userAnswer, String correctAnswer, List<String> options) {
        if (!isAvailable()) {
            return null;
        }

        String apiKey = getApiKey();
        String baseUrl = getBaseUrl();
        String model = getModel();
        int timeout = getTimeout();

        StringBuilder optionStr = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            optionStr.append((char) ('A' + i)).append(". ").append(options.get(i)).append("\n");
        }

        String systemPrompt = "你是一位耐心的儿童教育老师，正在给小学生讲解错题。请用简单易懂的语言解释为什么正确答案是对的，错误答案为什么不对。回答控制在100字以内，语气亲切鼓励。";

        String userPrompt = String.format(
            "题目：%s\n选项：\n%s学生选了：%s\n正确答案是：%s\n请解释这道题。",
            questionText, optionStr, userAnswer, correctAnswer
        );

        try {
            Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 300,
                "temperature", 0.7
            );

            String jsonBody = objectMapper.writeValueAsString(body);

            String url = baseUrl.endsWith("/v1") ? baseUrl + "/chat/completions" : baseUrl + "/v1/chat/completions";

            HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .body(jsonBody)
                .timeout(timeout * 1000)
                .execute();

            if (response.isOk()) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).get("message");
                    if (message != null && message.has("content")) {
                        return message.get("content").asText();
                    }
                }
            }
            log.warn("AI API returned non-OK status: {} body: {}", response.getStatus(), response.body());
            return null;
        } catch (Exception e) {
            log.error("AI explanation failed", e);
            return null;
        }
    }
}
