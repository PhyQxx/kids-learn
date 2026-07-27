package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.AppConfig;
import lombok.extern.slf4j.Slf4j;
import com.kidslearn.api.mapper.AppConfigMapper;
import com.kidslearn.api.service.AiService;
import com.kidslearn.api.service.impl.AdminOperationLogService;
import com.kidslearn.api.service.impl.QuestionAudioProperties;
import com.kidslearn.common.result.R;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "管理后台-AI配置")
@RestController
@RequestMapping("/api/v1/admin/ai")
@RequiredArgsConstructor
public class AdminAiConfigController {

    private static final String AI_PREFIX = "ai.";
    private static final String TTS_PREFIX = "tts.";
    private static final String AI_PROVIDER_KEY = "ai.provider";
    private static String aiImgProviderKey() { return "ai.image.provider"; }
    private static final String TTS_PROVIDER_KEY = "tts.provider";
    private static final String TIMEOUT_KEY = "ai.timeout";
    private static final int DEFAULT_TIMEOUT = 15;

    // provider, name, baseUrl, model, type(text/image/tts)
    private static final List<ProviderSeed> PROVIDER_SEEDS = List.of(
        // ---- 文字生成 ----
        new ProviderSeed("deepseek", "DeepSeek", "https://api.deepseek.com", "deepseek-chat", "text", ""),
        new ProviderSeed("openai", "OpenAI", "https://api.openai.com", "gpt-4o-mini", "text", ""),
        new ProviderSeed("qwen", "通义千问", "https://dashscope.aliyuncs.com/compatible-mode", "qwen-plus", "text", ""),
        new ProviderSeed("moonshot", "Moonshot", "https://api.moonshot.cn", "moonshot-v1-8k", "text", ""),
        new ProviderSeed("custom", "自定义兼容模型", "", "", "text", ""),
        // ---- 图片生成 ----
        new ProviderSeed("zhipu", "智谱 CogView", "https://open.bigmodel.cn/api/paas/v4", "cogview-3-flash", "image", ""),
        // ---- 语音合成 ----
        new ProviderSeed("mimo", "MiMo（云端）", "https://api.xiaomimimo.com/v1", "mimo-v2.5-tts", "tts", "冰糖"),
        new ProviderSeed("moss", "moss-tts（本地）", "/opt/moss-tts/bin/moss-tts-nano", "onnx", "tts", "Junhao")
    );

    private final AppConfigMapper appConfigMapper;
    private final AdminOperationLogService adminOperationLogService;
    private final AiService aiService;
    private final QuestionAudioProperties questionAudioProperties;

    // ===== 查询 =====

    @Operation(summary = "AI配置详情")
    @GetMapping("/config")
    public R<AiConfigResponse> detail() {
        Map<String, AppConfig> configs = loadAllConfigs();

        AiConfigResponse response = new AiConfigResponse();
        response.setProvider(val(configs, AI_PROVIDER_KEY, "deepseek"));
        response.setImageProvider(val(configs, aiImgProviderKey(), "zhipu"));
        response.setTimeout(parseTimeout(val(configs, TIMEOUT_KEY, String.valueOf(DEFAULT_TIMEOUT))));
        response.setCategories(buildCategories(configs));
        return R.ok(response);
    }

    // ===== 保存 =====

    @Operation(summary = "保存AI配置")
    @PostMapping("/config")
    public R<Void> save(@RequestBody AiConfigRequest request) {
        if (request == null) {
            return R.fail("请求不能为空");
        }

        Map<String, AppConfig> existing = loadAllConfigs();

        // 保存默认服务商
        if (!isBlank(request.getProvider()))        upsert(existing, AI_PROVIDER_KEY, request.getProvider().trim(), "文字默认服务商");
        if (!isBlank(request.getImageProvider()))    upsert(existing, aiImgProviderKey(), request.getImageProvider().trim(), "图片默认服务商");
        if (request.getTimeout() != null)            upsert(existing, TIMEOUT_KEY, String.valueOf(request.getTimeout()), "AI请求超时时间（秒）");

        // 保存所有 provider 配置（文字 + 图片 + TTS 统一处理）
        if (request.getProviders() != null) {
            for (ProviderConfig provider : request.getProviders()) {
                if (provider == null || isBlank(provider.getProvider())) continue;
                String providerKey = provider.getProvider().trim();
                if (!isProviderKey(providerKey)) {
                    return R.fail("服务商标识只能包含字母、数字、下划线和短横线");
                }
                String category = provider.getCategory() != null ? provider.getCategory().trim() : "text";
                log.info("Saving provider: key={}, category={}, name={}", providerKey, category, provider.getName());
                String prefix;
                if ("tts".equals(category)) {
                    prefix = TTS_PREFIX + providerKey + ".";
                } else if ("image".equals(category)) {
                    prefix = AI_PREFIX + providerKey + ".";
                } else {
                    prefix = AI_PREFIX + providerKey + ".";
                }
                upsert(existing, prefix + "enabled", provider.isEnabled() ? "1" : "0", providerKey + " 启用状态");
                upsert(existing, prefix + "base_url", trim(provider.getBaseUrl()), providerKey + " API地址");
                upsert(existing, prefix + "model", trim(provider.getModel()), providerKey + " 模型名称");
                // 保存分类信息（用于动态发现时判断归属）
                if (!"tts".equals(category)) {
                    upsert(existing, prefix + "category", category, providerKey + " 分类");
                }
                // 保存名称
                if (!isBlank(provider.getName())) {
                    upsert(existing, prefix + "name", provider.getName().trim(), providerKey + " 显示名称");
                }
                if (!isBlank(provider.getApiKey()) && !"******".equals(provider.getApiKey().trim())) {
                    upsert(existing, prefix + "api_key", provider.getApiKey().trim(), providerKey + " API Key");
                }
                // TTS 专有字段
                if ("tts".equals(category) && !isBlank(provider.getVoice())) {
                    upsert(existing, prefix + "voice", provider.getVoice().trim(), providerKey + " 语音");
                }
                // 文字生成专有字段
                if ("text".equals(category) && provider.getMaxTokens() != null && provider.getMaxTokens() > 0) {
                    upsert(existing, prefix + "max_tokens", String.valueOf(provider.getMaxTokens()), providerKey + " 最大Token数");
                }
            }
        }

        // 保存 TTS 默认引擎
        if (!isBlank(request.getTtsProvider())) {
            upsert(existing, TTS_PROVIDER_KEY, request.getTtsProvider().trim(), "TTS默认引擎");
        }

        questionAudioProperties.refresh();
        adminOperationLogService.write("ai-config", "save", "app-config", null, "saved");
        aiService.clearCache();
        return R.ok();
    }

    @Operation(summary = "测试AI服务商连接")
    @PostMapping("/test")
    public R<Map<String, Object>> testConnection(@RequestBody ProviderConfig provider) {
        if (provider == null || isBlank(provider.getProvider()) || isBlank(provider.getBaseUrl())) {
            return R.fail("请先填写服务商标识和 API 地址");
        }
        if ("tts".equals(provider.getCategory()) && !provider.getBaseUrl().startsWith("http")) {
            return R.ok(Map.of("reachable", true, "message", "本地命令路径格式有效，运行时由语音服务执行检查"));
        }
        String baseUrl = provider.getBaseUrl().replaceAll("/+$", "");
        String url = baseUrl.endsWith("/v1") ? baseUrl + "/models" : baseUrl + "/models";
        String apiKey = trim(provider.getApiKey());
        if (isBlank(apiKey)) {
            String prefix = "tts".equals(provider.getCategory()) ? TTS_PREFIX : AI_PREFIX;
            apiKey = val(loadAllConfigs(), prefix + provider.getProvider().trim() + ".api_key", "");
        }
        try (HttpResponse response = HttpRequest.get(url)
                .header("Authorization", "Bearer " + apiKey)
                .timeout(5000)
                .execute()) {
            int status = response.getStatus();
            if (status >= 200 && status < 300) {
                return R.ok(Map.of("reachable", true, "status", status, "message", "连接成功"));
            }
            return R.fail(status == 401 || status == 403 ? "认证失败，请检查 API Key" : "服务可访问，但返回 HTTP " + status);
        } catch (Exception error) {
            log.warn("AI provider connection test failed: {}", provider.getProvider(), error);
            return R.fail("连接失败：" + error.getMessage());
        }
    }

    // ===== 构建分类数据 =====

    private List<CategoryGroup> buildCategories(Map<String, AppConfig> configs) {
        Map<String, List<ProviderConfig>> grouped = new LinkedHashMap<>();
        grouped.put("text", new ArrayList<>());
        grouped.put("image", new ArrayList<>());
        grouped.put("tts", new ArrayList<>());

        // 从 seeds 构建
        Map<String, String> builtKeys = new LinkedHashMap<>();
        for (ProviderSeed seed : PROVIDER_SEEDS) {
            String prefix = "tts".equals(seed.type()) ? TTS_PREFIX : AI_PREFIX;
            ProviderConfig p = new ProviderConfig();
            p.setProvider(seed.provider());
            p.setName(seed.name());
            p.setCategory(seed.type());
            p.setBaseUrl(val(configs, prefix + seed.provider() + ".base_url", seed.baseUrl()));
            p.setModel(val(configs, prefix + seed.provider() + ".model", seed.model()));
            p.setEnabled("1".equals(val(configs, prefix + seed.provider() + ".enabled", "0")));
            p.setApiKey("");
            p.setApiKeyConfigured(!isBlank(val(configs, prefix + seed.provider() + ".api_key", "")));
            // TTS 专有字段
            if ("tts".equals(seed.type())) {
                p.setVoice(val(configs, prefix + seed.provider() + ".voice", seed.extra()));
            }
            // 文字生成专有字段
            if ("text".equals(seed.type())) {
                p.setMaxTokens(parseIntSafe(val(configs, prefix + seed.provider() + ".max_tokens", ""), 0));
            }
            builtKeys.put(seed.type() + ":" + seed.provider(), seed.type());
            grouped.get(seed.type()).add(p);
        }

        // 动态发现的 ai.* provider（文字/图片）
        // 第一步：收集所有 ai.* provider 的 key
        String imageDefaultProvider = val(configs, aiImgProviderKey(), "zhipu");
        Map<String, String> discoveredProviders = new LinkedHashMap<>();
        for (String configKey : configs.keySet()) {
            if (!configKey.startsWith(AI_PREFIX)) continue;
            String providerKey = parseProviderKey(configKey, AI_PREFIX);
            if (providerKey == null) continue;
            String lookupText = "text:" + providerKey;
            String lookupImage = "image:" + providerKey;
            if (builtKeys.containsKey(lookupText) || builtKeys.containsKey(lookupImage)) continue;
            discoveredProviders.put(providerKey, providerKey);
        }
        
        // 第二步：为每个发现的 provider 确定分类并添加
        log.info("Dynamic AI providers discovered: {}, imageDefaultProvider={}", discoveredProviders.keySet(), imageDefaultProvider);
        for (String providerKey : discoveredProviders.keySet()) {
            // 判断分类：检查 category 配置，或者是图片默认服务商
            String savedCategory = val(configs, AI_PREFIX + providerKey + ".category", "");
            log.info("Provider '{}': savedCategory='{}', isImageDefault={}", providerKey, savedCategory, providerKey.equals(imageDefaultProvider));
            String category;
            if ("image".equals(savedCategory)) {
                category = "image";
            } else if (providerKey.equals(imageDefaultProvider)) {
                category = "image";
            } else {
                category = "text";
            }
            log.info("Provider '{}' assigned to category: {}", providerKey, category);
            
            ProviderConfig p = new ProviderConfig();
            p.setProvider(providerKey);
            p.setName(val(configs, AI_PREFIX + providerKey + ".name", providerKey));
            p.setCategory(category);
            p.setBaseUrl(val(configs, AI_PREFIX + providerKey + ".base_url", ""));
            p.setModel(val(configs, AI_PREFIX + providerKey + ".model", ""));
            p.setEnabled("1".equals(val(configs, AI_PREFIX + providerKey + ".enabled", "0")));
            p.setApiKey("");
            p.setApiKeyConfigured(!isBlank(val(configs, AI_PREFIX + providerKey + ".api_key", "")));
            if ("text".equals(category)) {
                p.setMaxTokens(parseIntSafe(val(configs, AI_PREFIX + providerKey + ".max_tokens", ""), 0));
            }
            grouped.get(category).add(p);
            builtKeys.put(category + ":" + providerKey, category);
        }

        // 动态发现的 tts.* provider
        for (String configKey : configs.keySet()) {
            if (!configKey.startsWith(TTS_PREFIX)) continue;
            String providerKey = parseProviderKey(configKey, TTS_PREFIX);
            if (providerKey == null || "provider".equals(providerKey)) continue;
            if (builtKeys.containsKey("tts:" + providerKey)) continue;
            ProviderConfig p = new ProviderConfig();
            p.setProvider(providerKey);
            p.setName(providerKey);
            p.setCategory("tts");
            p.setBaseUrl(val(configs, TTS_PREFIX + providerKey + ".base_url", ""));
            p.setModel(val(configs, TTS_PREFIX + providerKey + ".model", ""));
            p.setEnabled("1".equals(val(configs, TTS_PREFIX + providerKey + ".enabled", "0")));
            p.setApiKey("");
            p.setApiKeyConfigured(!isBlank(val(configs, TTS_PREFIX + providerKey + ".api_key", "")));
            p.setVoice(val(configs, TTS_PREFIX + providerKey + ".voice", ""));
            grouped.get("tts").add(p);
        }

        // TTS 默认引擎
        String ttsProvider = val(configs, TTS_PROVIDER_KEY, "mimo");

        List<CategoryGroup> categories = new ArrayList<>();
        categories.add(new CategoryGroup("text", "文字生成", grouped.get("text"), val(configs, AI_PROVIDER_KEY, "deepseek")));
        categories.add(new CategoryGroup("image", "图片生成", grouped.get("image"), val(configs, aiImgProviderKey(), "zhipu")));
        categories.add(new CategoryGroup("tts", "语音合成", grouped.get("tts"), ttsProvider));
        return categories;
    }

    // ===== 工具方法 =====

    private Map<String, AppConfig> loadAllConfigs() {
        List<AppConfig> configs = appConfigMapper.selectList(
            new LambdaQueryWrapper<AppConfig>()
                .likeRight(AppConfig::getConfigKey, AI_PREFIX)
                .or()
                .likeRight(AppConfig::getConfigKey, TTS_PREFIX)
        );
        Map<String, AppConfig> map = new LinkedHashMap<>();
        for (AppConfig config : configs) {
            if (config.getConfigKey() != null) {
                map.put(config.getConfigKey(), config);
            }
        }
        return map;
    }

    private void upsert(Map<String, AppConfig> existing, String key, String value, String description) {
        AppConfig config = existing.get(key);
        if (config == null) {
            config = new AppConfig();
            config.setConfigKey(key);
            config.setConfigType(1);
            config.setDescription(description);
            config.setConfigValue(value == null ? "" : value);
            appConfigMapper.insert(config);
            existing.put(key, config);
            return;
        }
        config.setConfigValue(value == null ? "" : value);
        if (isBlank(config.getDescription())) config.setDescription(description);
        if (config.getConfigType() == null) config.setConfigType(1);
        appConfigMapper.updateById(config);
    }

    private static String val(Map<String, AppConfig> configs, String key, String defaultValue) {
        AppConfig config = configs.get(key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) return defaultValue;
        return config.getConfigValue().trim();
    }

    private static int parseTimeout(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return DEFAULT_TIMEOUT; }
    }

    private static int parseIntSafe(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

    private static String parseProviderKey(String configKey, String prefix) {
        if (configKey == null || !configKey.startsWith(prefix)) return null;
        String suffix = configKey.substring(prefix.length());
        int dot = suffix.indexOf('.');
        return dot <= 0 ? null : suffix.substring(0, dot);
    }

    private static boolean isProviderKey(String value) { return value.matches("[A-Za-z0-9_-]+"); }
    private static String trim(String value) { return value == null ? "" : value.trim(); }
    private static boolean isBlank(String value) { return value == null || value.isBlank(); }

    private record ProviderSeed(String provider, String name, String baseUrl, String model, String type, String extra) {}

    // ===== DTO =====

    @Data
    public static class AiConfigResponse {
        private String provider;        // 文字默认
        private String imageProvider;   // 图片默认
        private Integer timeout;
        private List<CategoryGroup> categories;
    }

    @Data
    public static class AiConfigRequest {
        private String provider;        // 文字默认
        private String imageProvider;   // 图片默认
        private String ttsProvider;     // TTS 默认引擎
        private Integer timeout;
        private List<ProviderConfig> providers;
    }

    @Data
    public static class CategoryGroup {
        private final String type;    // text / image / tts
        private final String label;   // 文字生成 / 图片生成 / 语音合成
        private final List<ProviderConfig> providers;
        private final String defaultProvider;  // 该分类的默认服务商
    }

    @Data
    public static class ProviderConfig {
        private String provider;
        private String name;
        private String category;   // text / image / tts
        private boolean enabled;
        private String baseUrl;
        private String model;
        private String apiKey;
        private boolean apiKeyConfigured;
        private String voice;      // TTS 专有
        private Integer maxTokens; // 文字生成最大 token
    }
}
