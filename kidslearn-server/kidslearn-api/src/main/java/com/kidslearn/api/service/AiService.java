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

        String systemPrompt = "你是一位耐心的儿童教育老师，正在给小学生讲解错题。请用简单易懂的语言解释为什么正确答案是对的，错误答案为什么不对。回答控制在100字以内，语气亲切鼓励。只输出解析正文，不要输出思考过程。";

        String userPrompt = String.format(
            "题目：%s\n选项：\n%s学生选了：%s\n正确答案是：%s\n请直接解释这道题，不要输出思考过程。",
            questionText, optionStr, userAnswer, correctAnswer
        );

        try {
            return chatCompletion(List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ), 2048, 0.7);
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
        String systemPrompt = "你是一位耐心的儿童教育老师。请把题目解析写得简短、准确、鼓励孩子理解，不要直接责备。只输出解析正文，不要输出思考过程。";
        String userPrompt = """
            题目：%s
            选项：%s
            正确答案：%s
            原解析：%s
            请直接输出100字以内的解析正文，不要输出你的思考过程。
            """.formatted(blankDefault(questionContent, "未提供"), optionText, blankDefault(correctAnswer, "未提供"), blankDefault(existingAnalysis, "无"));

        try {
            return chatCompletion(List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ), 2048, 0.5);
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

    /**
     * 生成答题反馈文字（答对/答错）
     * @param type "correct" 或 "wrong"
     * @param subject 学科，如 "数学"、"英语"
     * @return 生成的反馈文字，如 "太棒了！"、"加油，再想想！"
     */
    public String generateFeedbackText(String type, String subject) {
        if (!isAvailable()) {
            throw new BusinessException("AI 服务暂不可用");
        }

        boolean isCorrect = "correct".equals(type);
        String systemPrompt = "你是儿童学习平台的语音反馈助手。请生成简短、活泼、鼓励性的语音反馈文字，适合播放给小朋友听。只返回文字内容，不要输出任何其他内容。";
        String userPrompt = isCorrect
            ? "请为%s课答对题目的小朋友生成一句庆祝语音反馈。要求：5-15字，活泼兴奋，用庆祝类emoji（🎉👏✨🌟），比如'太棒了！🎉'、'答对啦！👏'、'你真聪明！✨'。语气要欢快。不要重复，随机生成一句。".formatted(subject != null ? subject : "")
            : "请为%s课答错题目的小朋友生成一句安慰语音反馈。要求：5-15字，语气温柔但要明确表示答错了，用加油类emoji（💪🤔😊），比如'答错啦，再想想！🤔'、'不对哦，加油！💪'、'嗯？不太对，再试一次！'。必须让小朋友听出是答错了，不能和答对的反馈混淆。不要重复，随机生成一句。".formatted(subject != null ? subject : "");

        try {
            String response = chatCompletion(List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ), 800, 0.9);
            if (response == null || response.isBlank()) {
                // AI 返回为空，使用默认文案
                return isCorrect ? "太棒了！🎉" : "答错啦，再想想！🤔";
            }
            // 清理可能的引号和多余空白
            return response.replaceAll("^[\"'\u201C\u201D]|[\u201C\u201D\"']$", "").trim();
        } catch (Exception e) {
            log.error("AI feedback text generation failed, using default", e);
            return isCorrect ? "太棒了！🎉" : "答错啦，再想想！🤔";
        }
    }

    private String chatCompletion(List<Map<String, String>> messages, int maxTokens, double temperature) throws Exception {
        String provider = getProvider();
        String apiKey = getConfig("ai." + provider + ".api_key");
        String baseUrl = getConfig("ai." + provider + ".base_url");
        String model = getConfig("ai." + provider + ".model");
        int timeout = getTimeout();

        // 服务商配置的 max_tokens 作为上限，取较小值
        String configuredMaxTokens = getConfig("ai." + provider + ".max_tokens");
        if (configuredMaxTokens != null && !configuredMaxTokens.isBlank()) {
            try {
                int parsed = Integer.parseInt(configuredMaxTokens.trim());
                if (parsed > 0) maxTokens = Math.min(maxTokens, parsed);
            } catch (NumberFormatException ignored) {}
        }

        // 推理模型需要更大的 token 预算，因为 reasoning_tokens 也计入 max_tokens
        // 可通过 ai.{provider}.reasoning_model 配置是否为推理模型，默认 false
        boolean isReasoningModel = "true".equalsIgnoreCase(getConfig("ai." + provider + ".reasoning_model"));
        if (isReasoningModel && maxTokens < 4096) {
            // 为推理模型自动增加 token 预算，确保有足够空间输出最终答案
            maxTokens = Math.min(maxTokens * 3, 8192);
            log.debug("推理模型自动增加 maxTokens 到 {}", maxTokens);
        }

        // 使用 HashMap 以便动态添加 reasoning_effort 参数
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);

        // 推理模型支持 reasoning_effort 参数，限制思考 token 消耗
        // 可通过 ai.{provider}.reasoning_effort 配置，默认 "low"
        String reasoningEffort = getConfig("ai." + provider + ".reasoning_effort");
        if (reasoningEffort == null || reasoningEffort.isBlank()) {
            reasoningEffort = "low";
        }
        body.put("reasoning_effort", reasoningEffort);

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
                    // 推理模型可能把所有 token 用于 reasoning_content，导致 content 为空
                    // 降级：从 reasoning_content 中提取最后的结论部分
                    String reasoningContent = message.path("reasoning_content").asText("");
                    if (!reasoningContent.isBlank()) {
                        String extracted = extractFromReasoning(reasoningContent);
                        if (extracted != null) {
                            log.warn("AI content empty, using reasoning_content fallback (length={})", extracted.length());
                            return extracted;
                        }
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

    /**
     * 从推理模型的 reasoning_content 中提取最终结论。
     * 推理模型可能将所有 token 用于内部思考，导致 content 为空。
     * 此方法尝试从冗长的推理过程中提取结论性内容。
     */
    private String extractFromReasoning(String reasoning) {
        if (reasoning == null || reasoning.isBlank()) return null;
        String text = reasoning.strip();

        // 策略1：查找明确的输出标记后的内容
        String[] outputMarkers = {"输出：", "最终解析：", "解析正文：", "答案：", "结论："};
        for (String marker : outputMarkers) {
            int idx = text.lastIndexOf(marker);
            if (idx >= 0) {
                String afterMarker = text.substring(idx + marker.length()).trim();
                // 取到下一个思考标记或段落结束
                String extracted = cutUntilThinking(afterMarker);
                if (extracted != null && extracted.length() >= 10) {
                    return truncate(extracted, 500);
                }
            }
        }

        // 策略2：查找包含结论性词汇的句子
        String[] conclusionPatterns = {
            "所以答案是", "答案是", "因此", "所以", "总的来说", "综上所述",
            "正确答案是", "答案为", "结果是"
        };
        for (String pattern : conclusionPatterns) {
            int idx = text.lastIndexOf(pattern);
            if (idx >= 0) {
                // 从这个位置往前找句子开头，往后找句子结尾
                String sentence = extractSentence(text, idx);
                if (sentence != null && sentence.length() >= 5 && sentence.length() <= 300) {
                    // 检查是否包含实质内容（数字、关键词等）
                    if (sentence.matches(".*\\d.*") || containsSubstantiveContent(sentence)) {
                        return sentence;
                    }
                }
            }
        }

        // 策略3：如果整个内容都很短，可能本身就是结论
        if (text.length() <= 200) {
            return text;
        }

        // 策略4：无法提取有效结论，返回 null 让调用方使用默认值
        log.debug("无法从推理内容中提取有效结论，内容长度={}", text.length());
        return null;
    }

    /**
     * 切割到下一个思考过程标记
     */
    private String cutUntilThinking(String text) {
        String[] thinkingPatterns = {
            "\n让我", "\n我需要", "\n我应该", "\n为了更准确", "\n或许这样",
            "\n既然用户", "\n回顾原解析", "\n草拟解析", "\n检查", "\n决定：",
            "\n最终，我需要", "\n但这样", "\n为了鼓励"
        };
        int earliest = text.length();
        for (String pattern : thinkingPatterns) {
            int idx = text.indexOf(pattern);
            if (idx >= 0 && idx < earliest) {
                earliest = idx;
            }
        }
        return text.substring(0, earliest).trim();
    }

    /**
     * 从指定位置提取完整句子
     */
    private String extractSentence(String text, int markerIdx) {
        // 往前找句子开头（句号、换行或开头）
        int start = markerIdx;
        for (int i = markerIdx - 1; i >= 0 && i >= markerIdx - 100; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '\n' || c == '！' || c == '？') {
                start = i + 1;
                break;
            }
            if (i == 0) {
                start = 0;
            }
        }

        // 往后找句子结尾
        int end = markerIdx;
        for (int i = markerIdx; i < text.length() && i < markerIdx + 300; i++) {
            char c = text.charAt(i);
            if (c == '。' || c == '\n' || c == '！' || c == '？') {
                end = i + 1;
                break;
            }
            end = i + 1;
        }

        return text.substring(start, end).trim();
    }

    /**
     * 检查是否包含实质性内容
     */
    private boolean containsSubstantiveContent(String text) {
        // 检查是否包含数字、常见关键词
        return text.matches(".*\\d.*") ||
               text.contains("次") || text.contains("个") || text.contains("正确") ||
               text.contains("答案") || text.contains("是");
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
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

    // ---- AI 图片生成 ----

    private static final String ZHIPU_IMAGE_URL = "https://open.bigmodel.cn/api/paas/v4/images/generations";
    private static final String ZHIPU_DEFAULT_MODEL = "cogview-3-flash";

    private static final String SENSENOVA_IMAGE_URL = "https://token.sensenova.cn/v1/images/generations";
    private static final String SENSENOVA_DEFAULT_MODEL = "sensenova-u1-fast";

    /**
     * AI 图片生成，支持智谱 CogView 和 SenseNova U1 Fast。
     * 根据 ai.image.provider 配置决定使用哪个服务商。
     *
     * @param prompt 图片描述
     * @param size   图片尺寸，如 "1024x1024"，null 则使用默认尺寸
     * @return FTP 上的公开图片 URL
     */
    public String generateImage(String prompt, String size) {
        String provider = getConfig("ai.image.provider");
        if (provider == null || provider.isBlank() || "zhipu".equals(provider)) {
            return generateImageZhipu(prompt, size);
        } else if ("sensenova".equals(provider)) {
            return generateImageSenseNova(prompt, size);
        } else {
            throw new BusinessException("不支持的图片生成服务商: " + provider);
        }
    }

    /**
     * 调用智谱 CogView 生成图片
     */
    private String generateImageZhipu(String prompt, String size) {
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

            return downloadAndUpload(imageUrl);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Zhipu image generation failed", e);
            throw new BusinessException("AI 图片生成失败: " + e.getMessage());
        }
    }

    /**
     * 调用 SenseNova U1 Fast 生成图片
     */
    private String generateImageSenseNova(String prompt, String size) {
        String apiKey = getConfig("ai.sensenova.api_key");
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("SenseNova API Key 未配置 (ai.sensenova.api_key)");
        }
        String model = getConfig("ai.sensenova.image_model");
        if (model == null || model.isBlank()) {
            model = SENSENOVA_DEFAULT_MODEL;
        }
        // SenseNova U1 Fast 默认尺寸为 2752x1536 (16:9)
        if (size == null || size.isBlank()) {
            size = "2752x1536";
        } else {
            // 将通用尺寸映射到 SenseNova 支持的尺寸
            size = mapSizeToSenseNova(size);
        }

        try {
            Map<String, Object> body = Map.of("model", model, "prompt", prompt, "size", size);
            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("SenseNova image request model={} size={} prompt={}", model, size, prompt);

            HttpResponse response = HttpRequest.post(SENSENOVA_IMAGE_URL)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .body(jsonBody)
                .timeout(60_000)
                .execute();

            String responseBody = response.body();
            log.info("SenseNova image response status={}", response.getStatus());

            if (!response.isOk()) {
                log.error("SenseNova image API error, status={} body={}", response.getStatus(), responseBody);
                throw new BusinessException("AI 图片生成失败: HTTP " + response.getStatus());
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String imageUrl = root.path("data").path(0).path("url").asText("");
            if (imageUrl.isBlank()) {
                log.error("SenseNova image response missing url, body={}", responseBody);
                throw new BusinessException("AI 图片生成失败: 未返回图片 URL");
            }

            // SenseNova 返回的 URL 是临时链接（1小时有效），需要尽快下载
            return downloadAndUpload(imageUrl);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("SenseNova image generation failed", e);
            throw new BusinessException("AI 图片生成失败: " + e.getMessage());
        }
    }

    /**
     * 将通用尺寸映射到 SenseNova 支持的 2K 分辨率尺寸
     * SenseNova 支持的尺寸：
     *   1664x2496 (2:3), 2496x1664 (3:2)
     *   1760x2368 (3:4), 2368x1760 (4:3)
     *   1824x2272 (4:5), 2272x1824 (5:4)
     *   2048x2048 (1:1)
     *   2752x1536 (16:9), 1536x2752 (9:16)
     *   3072x1376 (21:9), 1344x3136 (9:21)
     */
    private String mapSizeToSenseNova(String size) {
        switch (size) {
            case "1024x1024": return "2048x2048";  // 1:1
            case "1024x768":  return "2368x1760";  // 4:3
            case "768x1024":  return "1760x2368";  // 3:4
            case "1920x1080": return "2752x1536";  // 16:9
            case "1080x1920": return "1536x2752";  // 9:16
            default: return "2048x2048";  // 默认 1:1
        }
    }

    /**
     * 下载图片并上传到 FTP
     */
    private String downloadAndUpload(String imageUrl) throws Exception {
        // 下载图片
        HttpResponse imgResponse = HttpRequest.get(imageUrl)
            .timeout(30_000)
            .execute();
        if (!imgResponse.isOk()) {
            throw new BusinessException("AI 图片下载失败: HTTP " + imgResponse.getStatus());
        }
        byte[] imageBytes = imgResponse.bodyBytes();

        // 上传到 FTP
        String serviceDir = "/question/images/ai";
        String fileName = FILE_TIME.format(LocalDateTime.now()) + ".png";
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            fileName = ftpTool.upload(serviceDir, fileName, is);
        }
        String publicUrl = ftpTool.buildPublicUrl(serviceDir, fileName);
        log.info("AI image saved to FTP: {}", publicUrl);
        return publicUrl;
    }
}
