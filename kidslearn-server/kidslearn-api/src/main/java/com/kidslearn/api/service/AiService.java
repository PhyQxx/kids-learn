package com.kidslearn.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.kidslearn.api.entity.AppConfig;
import com.kidslearn.api.mapper.AppConfigMapper;
import com.kidslearn.common.exception.BusinessException;
import com.kidslearn.common.ftp.FtpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AppConfigMapper appConfigMapper;
    private final FtpTool ftpTool;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, String> configCache = new ConcurrentHashMap<>();
    private long cacheTime = 0;
    private static final long CACHE_TTL_MS = 60_000; // 1分钟缓存

    public AiService(AppConfigMapper appConfigMapper, FtpTool ftpTool) {
        this.appConfigMapper = appConfigMapper;
        this.ftpTool = ftpTool;
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
            return chatCompletion(List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ), 300, 0.7);
        } catch (Exception e) {
            log.error("AI explanation failed", e);
            return null;
        }
    }

    public Map<String, Object> generateQuestionDraft(
        String subjectName,
        String gradeName,
        Integer questionType,
        String knowledgePoint
    ) {
        if (!isAvailable()) {
            return Map.of();
        }

        String typeName = questionTypeName(questionType);
        String systemPrompt = "你是儿童学习平台的题库编辑。请生成适合儿童的单道题，内容安全、语言简洁、答案唯一。只返回JSON，不要解释。";
        String userPrompt = """
            请生成一道题目草稿。
            学科：%s
            年级：%s
            题型：%s
            知识点：%s

            JSON格式：
            {
              "questionContent": "题干文本",
              "analysis": "100字以内儿童友好解析",
              "options": [
                {"optionLabel":"A","optionContent":"选项文本","isCorrect":1,"sortOrder":0}
              ]
            }
            选择题给4个选项；判断题给“正确/错误”；填空题给1-3个可接受答案；排序题/连线题给3-5项。
            """.formatted(blankDefault(subjectName, "未指定"), blankDefault(gradeName, "未指定"), typeName, blankDefault(knowledgePoint, "基础练习"));

        try {
            String content = chatCompletion(List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ), 900, 0.5);
            return normalizeQuestionDraft(content, questionType);
        } catch (Exception e) {
            log.error("AI question generation failed", e);
            return Map.of();
        }
    }

    public String generateQuestionAnalysis(
        String questionContent,
        String correctAnswer,
        List<String> options,
        String existingAnalysis
    ) {
        if (!isAvailable()) {
            return null;
        }

        String optionText = options == null || options.isEmpty() ? "无" : String.join("；", options);
        String systemPrompt = "你是一位耐心的儿童教育老师。请把题目解析写得简短、准确、鼓励孩子理解，不要直接责备。";
        String userPrompt = """
            题目：%s
            选项：%s
            正确答案：%s
            原解析：%s
            请输出100字以内解析，只返回解析正文。
            """.formatted(blankDefault(questionContent, "未提供"), optionText, blankDefault(correctAnswer, "未提供"), blankDefault(existingAnalysis, "无"));

        try {
            return chatCompletion(List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ), 300, 0.5);
        } catch (Exception e) {
            log.error("AI analysis generation failed", e);
            return null;
        }
    }

    public Map<String, Object> precheckContent(String targetType, String content) {
        if (!isAvailable()) {
            return Map.of();
        }

        String systemPrompt = "你是儿童学习平台内容安全与题库质量审核助手。只返回JSON，不要替审核员作最终决定。";
        String userPrompt = """
            请预审以下内容，检查：
            1. 是否适合儿童；
            2. 是否包含暴力、恐吓、歧视、成人、不良引导；
            3. 题目是否表述清晰；
            4. 答案是否唯一且解析是否准确。

            对象类型：%s
            内容：
            %s

            JSON格式：
            {
              "riskLevel": "LOW|MEDIUM|HIGH",
              "summary": "一句话结论",
              "issues": ["问题1"],
              "suggestions": ["建议1"]
            }
            """.formatted(blankDefault(targetType, "UNKNOWN"), blankDefault(content, "无内容"));

        try {
            String response = chatCompletion(List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ), 700, 0.2);
            return normalizePrecheckResult(response);
        } catch (Exception e) {
            log.error("AI content precheck failed", e);
            return Map.of();
        }
    }

    public Map<String, Object> generateParentSummary(Map<String, Object> report, Map<String, Object> monitor) {
        if (!isAvailable()) {
            return Map.of();
        }

        String systemPrompt = "你是儿童学习平台的家庭学习顾问。请给家长提供温和、可执行的学习总结，不制造焦虑，不做医疗或心理诊断。只返回JSON，不要输出思考过程。";
        String userPrompt;
        try {
            userPrompt = """
                请根据学习报告，为家长生成学习建议。
                要求：
                1. 总结控制在80字以内；
                2. 亮点、关注点、建议各1-3条；
                3. 建议要具体、温和、可执行；
                4. 不要泄露技术字段名。

                学习报告JSON：
                %s

                JSON格式：
                {
                  "summary": "一句话总结",
                  "highlights": ["亮点1"],
                  "concerns": ["关注点1"],
                  "suggestions": ["建议1"]
                }
                """.formatted(objectMapper.writeValueAsString(report));
        } catch (Exception e) {
            log.error("Parent summary prompt build failed", e);
            return fallbackParentSummary(report);
        }

        try {
            String response = chatCompletion(List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ), 1200, 0.3);
            Map<String, Object> result = normalizeParentSummary(response);
            return result.isEmpty() ? fallbackParentSummary(report) : result;
        } catch (Exception e) {
            log.error("AI parent summary unavailable, using local fallback", e);
            return fallbackParentSummary(report);
        }
    }

    private String chatCompletion(List<Map<String, String>> messages, int maxTokens, double temperature) throws Exception {
        String provider = getProvider();
        String apiKey = getConfig("ai." + provider + ".api_key");
        String baseUrl = getConfig("ai." + provider + ".base_url");
        String model = getConfig("ai." + provider + ".model");
        int timeout = getTimeout();

        Map<String, Object> body = Map.of(
            "model", model,
            "messages", messages,
            "max_tokens", maxTokens,
            "temperature", temperature
        );

        String jsonBody = objectMapper.writeValueAsString(body);

        String url = baseUrl.endsWith("/v1") ? baseUrl + "/chat/completions" : baseUrl + "/v1/chat/completions";
        log.info("AI chat request provider={} model={} url={} maxTokens={} temperature={} body={}",
            provider, model, url, maxTokens, temperature, jsonBody);

        HttpResponse response = HttpRequest.post(url)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .body(jsonBody)
            .timeout(timeout * 1000)
            .execute();

        String responseBody = response.body();
        log.info("AI chat response provider={} model={} status={} body={}",
            provider, model, response.getStatus(), responseBody);

        if (response.isOk()) {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode choice = choices.get(0);
                JsonNode message = choice.get("message");
                if (message != null && message.has("content")) {
                    String content = message.get("content").asText();
                    if (!content.isBlank()) {
                        return content;
                    }
                    String finishReason = choice.path("finish_reason").asText("");
                    log.error("AI API returned empty assistant content, finish_reason={} body={}", finishReason, responseBody);
                    return null;
                }
            }
        }
        log.error("AI API returned invalid response, status: {} body: {}", response.getStatus(), responseBody);
        return null;
    }

    private Map<String, Object> normalizePrecheckResult(String content) throws Exception {
        if (content == null || content.isBlank()) {
            return Map.of();
        }
        JsonNode root = objectMapper.readTree(stripJsonFence(content));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("riskLevel", root.path("riskLevel").asText("MEDIUM"));
        result.put("summary", root.path("summary").asText(""));
        result.put("issues", objectMapper.convertValue(
            root.path("issues"),
            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        ));
        result.put("suggestions", objectMapper.convertValue(
            root.path("suggestions"),
            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        ));
        return result;
    }

    private Map<String, Object> normalizeParentSummary(String content) throws Exception {
        if (content == null || content.isBlank()) {
            return Map.of();
        }
        JsonNode root = objectMapper.readTree(stripJsonFence(content));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", root.path("summary").asText(""));
        result.put("highlights", objectMapper.convertValue(
            root.path("highlights"),
            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        ));
        result.put("concerns", objectMapper.convertValue(
            root.path("concerns"),
            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        ));
        result.put("suggestions", objectMapper.convertValue(
            root.path("suggestions"),
            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        ));
        return result;
    }

    private Map<String, Object> fallbackParentSummary(Map<String, Object> report) {
        Map<String, Object> today = mapValue(report, "today");
        Map<String, Object> stats = mapValue(report, "stats");
        int todayMinutes = intValue(today.get("learnMinutes"));
        int completedLevels = intValue(today.get("completedLevels"));
        int accuracy = intValue(today.get("accuracy"));
        int monthMinutes = intValue(stats.get("totalTime"));

        List<String> highlights = new java.util.ArrayList<>();
        if (todayMinutes > 0) {
            highlights.add("今天已经完成 " + todayMinutes + " 分钟学习。");
        }
        if (completedLevels > 0) {
            highlights.add("今天完成了 " + completedLevels + " 个关卡。");
        }
        if (accuracy >= 80) {
            highlights.add("今日正确率较稳定，可以继续保持。");
        }
        if (highlights.isEmpty()) {
            highlights.add("可以从一个轻松的小练习开始建立节奏。");
        }

        List<String> concerns = new java.util.ArrayList<>();
        if (accuracy > 0 && accuracy < 60) {
            concerns.add("今日正确率偏低，建议先复盘错题。");
        } else if (todayMinutes >= 50) {
            concerns.add("今日学习时间较长，注意安排休息。");
        } else {
            concerns.add("暂无明显风险，继续观察学习节奏。");
        }

        List<String> suggestions = new java.util.ArrayList<>();
        suggestions.add(todayMinutes > 0 ? "结束后和孩子一起回顾一道印象最深的题。" : "先安排 10 分钟左右的短练习，降低开始难度。");
        suggestions.add(monthMinutes > 0 ? "结合本月学习记录，优先保持固定的小段学习时间。" : "完成首次练习后再观察兴趣点和薄弱点。");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", todayMinutes > 0
            ? "AI服务响应较慢，已根据现有学习报告生成本地建议。"
            : "AI服务响应较慢，建议先从轻量练习开始。");
        result.put("highlights", highlights);
        result.put("concerns", concerns);
        result.put("suggestions", suggestions);
        return result;
    }

    private Map<String, Object> mapValue(Map<String, Object> source, String key) {
        if (source == null || !(source.get(key) instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() != null) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return result;
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private Map<String, Object> normalizeQuestionDraft(String content, Integer questionType) throws Exception {
        if (content == null || content.isBlank()) {
            return Map.of();
        }
        String json = stripJsonFence(content);
        JsonNode root = objectMapper.readTree(json);
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("questionType", questionType == null ? 1 : questionType);
        draft.put("questionContent", root.path("questionContent").asText(""));
        draft.put("analysis", root.path("analysis").asText(""));

        List<Map<String, Object>> options = objectMapper.convertValue(
            root.path("options"),
            objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
        );
        draft.put("options", options == null ? List.of() : options);
        if (draft.get("questionContent").toString().isBlank()) {
            return Map.of();
        }
        return draft;
    }

    private static String stripJsonFence(String content) {
        String text = content.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private static String questionTypeName(Integer type) {
        return switch (type == null ? 1 : type) {
            case 2 -> "判断题";
            case 3 -> "填空题";
            case 4 -> "排序题";
            case 5 -> "连线题";
            default -> "选择题";
        };
    }

    private static String blankDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    // ---- AI 图片生成 (智谱 CogView) ----

    private static final String ZHIPU_IMAGE_URL = "https://open.bigmodel.cn/api/paas/v4/images/generations";
    private static final String ZHIPU_DEFAULT_MODEL = "cogview-3-flash";

    /**
     * 调用智谱 CogView 生成图片，下载后上传到 FTP，返回永久可访问 URL。
     *
     * @param prompt 图片描述
     * @param size   图片尺寸，如 "1024x1024"，null 则默认 1024x1024
     * @return FTP 上的公开图片 URL
     */
    public String generateImage(String prompt, String size) {
        String apiKey = getConfig("ai.zhipu.api_key");
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("智谱 API Key 未配置 (ai.zhipu.api_key)");
        }
        String model = getConfig("ai.zhipu.image_model");
        if (model == null || model.isBlank()) {
            model = ZHIPU_DEFAULT_MODEL;
        }
        if (size == null || size.isBlank()) {
            size = "1024x1024";
        }

        try {
            // 1. 调用智谱图片生成 API
            Map<String, Object> body = Map.of("model", model, "prompt", prompt, "size", size);
            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("Zhipu image request model={} size={} prompt={}", model, size, prompt);

            HttpResponse response = HttpRequest.post(ZHIPU_IMAGE_URL)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .body(jsonBody)
                .timeout(60_000)
                .execute();

            String responseBody = response.body();
            log.info("Zhipu image response status={}", response.getStatus());

            if (!response.isOk()) {
                log.error("Zhipu image API error, status={} body={}", response.getStatus(), responseBody);
                throw new BusinessException("AI 图片生成失败: HTTP " + response.getStatus());
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String imageUrl = root.path("data").path(0).path("url").asText("");
            if (imageUrl.isBlank()) {
                log.error("Zhipu image response missing url, body={}", responseBody);
                throw new BusinessException("AI 图片生成失败: 未返回图片 URL");
            }

            // 2. 下载图片
            HttpResponse imgResponse = HttpRequest.get(imageUrl)
                .timeout(30_000)
                .execute();
            if (!imgResponse.isOk()) {
                throw new BusinessException("AI 图片下载失败: HTTP " + imgResponse.getStatus());
            }
            byte[] imageBytes = imgResponse.bodyBytes();

            // 3. 上传到 FTP
            String serviceDir = "/question/images/ai";
            String fileName = FILE_TIME.format(LocalDateTime.now()) + ".png";
            try (InputStream is = new ByteArrayInputStream(imageBytes)) {
                fileName = ftpTool.upload(serviceDir, fileName, is);
            }
            String publicUrl = ftpTool.buildPublicUrl(serviceDir, fileName);
            log.info("AI image saved to FTP: {}", publicUrl);
            return publicUrl;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI image generation failed", e);
            throw new BusinessException("AI 图片生成失败: " + e.getMessage());
        }
    }
}
