package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.AppConfig;
import com.kidslearn.api.mapper.AppConfigMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * TTS 配置，从 app_config 表读取，带 1 分钟内存缓存。
 * 配置项前缀: tts.
 */
@Component
public class QuestionAudioProperties {

    private static final String PREFIX = "tts.";
    private static final long CACHE_TTL_MS = 60_000;

    private final AppConfigMapper appConfigMapper;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile long cacheTime = 0;

    // 用 @Getter 保持对外 API 不变
    @Getter private String engine = "mimo";
    @Getter private String ttsCommand = "/opt/moss-tts/bin/moss-tts-nano";
    @Getter private String backend = "onnx";
    @Getter private String voice = "Junhao";
    @Getter private String mimoApiKey;
    @Getter private String mimoBaseUrl = "https://api.xiaomimimo.com/v1";
    @Getter private String mimoModel = "mimo-v2.5-tts";
    @Getter private String mimoVoice = "冰糖";
    @Getter private String tempDir = System.getProperty("java.io.tmpdir");
    @Getter private Duration timeout = Duration.ofMinutes(5);

    public QuestionAudioProperties(AppConfigMapper appConfigMapper) {
        this.appConfigMapper = appConfigMapper;
    }

    /** 刷新缓存并回填字段，外部可直接调用 getter */
    public void refresh() {
        cacheTime = 0;
        loadFromDb();
    }

    private String get(String key, String defaultValue) {
        ensureLoaded();
        return cache.getOrDefault(PREFIX + key, defaultValue);
    }

    private synchronized void ensureLoaded() {
        long now = System.currentTimeMillis();
        if (!cache.isEmpty() && now - cacheTime <= CACHE_TTL_MS) {
            return;
        }
        loadFromDb();
    }

    private void loadFromDb() {
        cache.clear();
        List<AppConfig> configs = appConfigMapper.selectList(
            new LambdaQueryWrapper<AppConfig>().likeRight(AppConfig::getConfigKey, PREFIX)
        );
        for (AppConfig c : configs) {
            if (c.getConfigKey() != null && c.getConfigValue() != null) {
                cache.put(c.getConfigKey(), c.getConfigValue().trim());
            }
        }
        cacheTime = System.currentTimeMillis();

        // 回填字段
        this.engine = get("provider", get("engine", "mimo"));
        this.ttsCommand = get("moss.base_url", get("moss.command", "/opt/moss-tts/bin/moss-tts-nano"));
        this.backend = get("moss.model", get("moss.backend", "onnx"));
        this.voice = get("moss.voice", "Junhao");
        this.mimoApiKey = get("mimo.api_key", "");
        this.mimoBaseUrl = get("mimo.base_url", "https://api.xiaomimimo.com/v1");
        this.mimoModel = get("mimo.model", "mimo-v2.5-tts");
        this.mimoVoice = get("mimo.voice", "冰糖");
        this.tempDir = get("temp_dir", System.getProperty("java.io.tmpdir"));
        try {
            long ms = Long.parseLong(get("timeout", "300000"));
            this.timeout = Duration.ofMillis(ms);
        } catch (NumberFormatException e) {
            this.timeout = Duration.ofMinutes(5);
        }
    }
}
