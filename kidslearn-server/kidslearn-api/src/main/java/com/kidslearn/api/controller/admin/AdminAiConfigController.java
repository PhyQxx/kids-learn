package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.AppConfig;
import com.kidslearn.api.mapper.AppConfigMapper;
import com.kidslearn.api.service.AiService;
import com.kidslearn.api.service.impl.AdminOperationLogService;
import com.kidslearn.common.result.R;
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

@Tag(name = "管理后台-AI配置")
@RestController
@RequestMapping("/api/v1/admin/ai")
@RequiredArgsConstructor
public class AdminAiConfigController {

    private static final String PREFIX = "ai.";
    private static final String ACTIVE_PROVIDER_KEY = "ai.provider";
    private static final String TIMEOUT_KEY = "ai.timeout";
    private static final int DEFAULT_TIMEOUT = 15;
    private static final List<ProviderSeed> PROVIDER_SEEDS = List.of(
        new ProviderSeed("deepseek", "DeepSeek", "https://api.deepseek.com", "deepseek-chat"),
        new ProviderSeed("openai", "OpenAI", "https://api.openai.com", "gpt-4o-mini"),
        new ProviderSeed("qwen", "通义千问", "https://dashscope.aliyuncs.com/compatible-mode", "qwen-plus"),
        new ProviderSeed("moonshot", "Moonshot", "https://api.moonshot.cn", "moonshot-v1-8k"),
        new ProviderSeed("custom", "自定义兼容模型", "", "")
    );

    private final AppConfigMapper appConfigMapper;
    private final AdminOperationLogService adminOperationLogService;
    private final AiService aiService;

    @Operation(summary = "AI配置详情")
    @GetMapping("/config")
    public R<AiConfigResponse> detail() {
        Map<String, AppConfig> configs = loadAiConfigs();
        Map<String, ProviderConfig> providers = buildProviderMap(configs);
        String provider = value(configs, ACTIVE_PROVIDER_KEY, "deepseek");

        AiConfigResponse response = new AiConfigResponse();
        response.setProvider(provider);
        response.setTimeout(parseTimeout(value(configs, TIMEOUT_KEY, String.valueOf(DEFAULT_TIMEOUT))));
        response.setProviders(new ArrayList<>(providers.values()));
        return R.ok(response);
    }

    @Operation(summary = "保存AI配置")
    @PostMapping("/config")
    public R<Void> save(@RequestBody AiConfigRequest request) {
        if (request == null || isBlank(request.getProvider())) {
            return R.fail("请选择AI服务商");
        }
        if (request.getTimeout() == null || request.getTimeout() < 1 || request.getTimeout() > 120) {
            return R.fail("超时时间需在1-120秒之间");
        }
        if (request.getProviders() == null || request.getProviders().isEmpty()) {
            return R.fail("请至少配置一个AI服务商");
        }

        Map<String, AppConfig> existing = loadAiConfigs();
        upsert(existing, ACTIVE_PROVIDER_KEY, request.getProvider().trim(), "当前AI服务商");
        upsert(existing, TIMEOUT_KEY, String.valueOf(request.getTimeout()), "AI请求超时时间（秒）");

        for (ProviderConfig provider : request.getProviders()) {
            if (provider == null || isBlank(provider.getProvider())) {
                continue;
            }
            String providerKey = provider.getProvider().trim();
            if (!isProviderKey(providerKey)) {
                return R.fail("AI服务商标识只能包含字母、数字、下划线和短横线");
            }
            String prefix = PREFIX + providerKey + ".";
            upsert(existing, prefix + "enabled", provider.isEnabled() ? "1" : "0", providerKey + " 启用状态");
            upsert(existing, prefix + "base_url", trim(provider.getBaseUrl()), providerKey + " API地址");
            upsert(existing, prefix + "model", trim(provider.getModel()), providerKey + " 模型名称");
            if (!isBlank(provider.getApiKey()) && !"******".equals(provider.getApiKey().trim())) {
                upsert(existing, prefix + "api_key", provider.getApiKey().trim(), providerKey + " API Key");
            }
        }

        adminOperationLogService.write("ai-config", "save", "app-config", null, "provider=" + request.getProvider().trim());
        aiService.clearCache();
        return R.ok();
    }

    private Map<String, AppConfig> loadAiConfigs() {
        List<AppConfig> configs = appConfigMapper.selectList(
            new LambdaQueryWrapper<AppConfig>().likeRight(AppConfig::getConfigKey, PREFIX)
        );
        Map<String, AppConfig> map = new LinkedHashMap<>();
        for (AppConfig config : configs) {
            if (config.getConfigKey() != null) {
                map.put(config.getConfigKey(), config);
            }
        }
        return map;
    }

    private Map<String, ProviderConfig> buildProviderMap(Map<String, AppConfig> configs) {
        Map<String, ProviderConfig> providers = new LinkedHashMap<>();
        for (ProviderSeed seed : PROVIDER_SEEDS) {
            ProviderConfig provider = new ProviderConfig();
            provider.setProvider(seed.provider());
            provider.setName(seed.name());
            provider.setBaseUrl(value(configs, key(seed.provider(), "base_url"), seed.baseUrl()));
            provider.setModel(value(configs, key(seed.provider(), "model"), seed.model()));
            provider.setEnabled("1".equals(value(configs, key(seed.provider(), "enabled"), "0")));
            provider.setApiKey("");
            provider.setApiKeyConfigured(!isBlank(value(configs, key(seed.provider(), "api_key"), "")));
            providers.put(seed.provider(), provider);
        }

        for (String configKey : configs.keySet()) {
            String providerKey = parseProviderKey(configKey);
            if (providerKey == null || providers.containsKey(providerKey)) {
                continue;
            }
            ProviderConfig provider = new ProviderConfig();
            provider.setProvider(providerKey);
            provider.setName(providerKey);
            provider.setBaseUrl(value(configs, key(providerKey, "base_url"), ""));
            provider.setModel(value(configs, key(providerKey, "model"), ""));
            provider.setEnabled("1".equals(value(configs, key(providerKey, "enabled"), "0")));
            provider.setApiKey("");
            provider.setApiKeyConfigured(!isBlank(value(configs, key(providerKey, "api_key"), "")));
            providers.put(providerKey, provider);
        }
        return providers;
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
        if (isBlank(config.getDescription())) {
            config.setDescription(description);
        }
        if (config.getConfigType() == null) {
            config.setConfigType(1);
        }
        appConfigMapper.updateById(config);
    }

    private static String key(String provider, String field) {
        return PREFIX + provider + "." + field;
    }

    private static String value(Map<String, AppConfig> configs, String key, String defaultValue) {
        AppConfig config = configs.get(key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return defaultValue;
        }
        return config.getConfigValue().trim();
    }

    private static int parseTimeout(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return DEFAULT_TIMEOUT;
        }
    }

    private static String parseProviderKey(String configKey) {
        if (configKey == null || !configKey.startsWith(PREFIX)) {
            return null;
        }
        String suffix = configKey.substring(PREFIX.length());
        int dot = suffix.indexOf('.');
        if (dot <= 0) {
            return null;
        }
        return suffix.substring(0, dot);
    }

    private static boolean isProviderKey(String value) {
        return value.matches("[A-Za-z0-9_-]+");
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ProviderSeed(String provider, String name, String baseUrl, String model) {}

    @Data
    public static class AiConfigResponse {
        private String provider;
        private Integer timeout;
        private List<ProviderConfig> providers;
    }

    @Data
    public static class AiConfigRequest {
        private String provider;
        private Integer timeout;
        private List<ProviderConfig> providers;
    }

    @Data
    public static class ProviderConfig {
        private String provider;
        private String name;
        private boolean enabled;
        private String baseUrl;
        private String model;
        private String apiKey;
        private boolean apiKeyConfigured;
    }
}
