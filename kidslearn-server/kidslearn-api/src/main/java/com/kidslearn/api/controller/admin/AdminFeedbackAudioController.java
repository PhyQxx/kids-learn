package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.AppConfig;
import com.kidslearn.api.mapper.AppConfigMapper;
import com.kidslearn.api.service.AiService;
import com.kidslearn.api.service.impl.QuestionAudioGenerator;
import com.kidslearn.api.service.impl.QuestionAudioProperties;
import com.kidslearn.common.exception.BusinessException;
import com.kidslearn.common.ftp.FtpTool;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "管理后台-反馈语音配置")
@RestController
@RequestMapping("/api/v1/admin/feedback-audio")
@RequiredArgsConstructor
public class AdminFeedbackAudioController {

    private static final String PREFIX = "feedback.audio.";
    private static final String KEY_BASE_URL = PREFIX + "base_url";
    private static final String KEY_CORRECT_LIST = PREFIX + "correct_list";
    private static final String KEY_WRONG_LIST = PREFIX + "wrong_list";

    private static final String DEFAULT_BASE_URL = "https://ftp.pnkx.top:8/ftp/kids-learn/question/audio/seed/feedback";
    private static final String DEFAULT_CORRECT_LIST = "correct-1,correct-2,correct-3,correct-4,correct-5";
    private static final String DEFAULT_WRONG_LIST = "wrong-1,wrong-2,wrong-3";

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AppConfigMapper appConfigMapper;
    private final AiService aiService;
    private final Map<String, QuestionAudioGenerator> generators;
    private final QuestionAudioProperties audioProperties;
    private final FtpTool ftpTool;

    @Operation(summary = "获取反馈语音配置")
    @GetMapping("/config")
    public R<FeedbackAudioConfig> detail() {
        Map<String, AppConfig> configs = loadConfigs();
        FeedbackAudioConfig response = new FeedbackAudioConfig();
        response.setBaseUrl(val(configs, KEY_BASE_URL, DEFAULT_BASE_URL));
        response.setCorrectList(val(configs, KEY_CORRECT_LIST, DEFAULT_CORRECT_LIST));
        response.setWrongList(val(configs, KEY_WRONG_LIST, DEFAULT_WRONG_LIST));
        return R.ok(response);
    }

    @Operation(summary = "保存反馈语音配置")
    @PostMapping("/config")
    public R<Void> save(@RequestBody FeedbackAudioConfig request) {
        if (request == null) {
            return R.fail("请求不能为空");
        }

        Map<String, AppConfig> existing = loadConfigs();

        if (request.getBaseUrl() != null) {
            upsert(existing, KEY_BASE_URL, request.getBaseUrl().trim(), "反馈语音基础URL");
        }
        if (request.getCorrectList() != null) {
            upsert(existing, KEY_CORRECT_LIST, request.getCorrectList().trim(), "答对反馈语音列表");
        }
        if (request.getWrongList() != null) {
            upsert(existing, KEY_WRONG_LIST, request.getWrongList().trim(), "答错反馈语音列表");
        }

        return R.ok();
    }

    @Operation(summary = "AI生成反馈语音")
    @PostMapping("/generate")
    public R<GenerateResult> generate(@RequestBody GenerateRequest request) {
        if (request == null || request.getType() == null) {
            return R.fail("请求不能为空");
        }

        String type = request.getType(); // "correct" 或 "wrong"
        String subject = request.getSubject(); // 学科，可选
        String text = request.getText(); // 自定义文字，可选

        try {
            // 1. 确定文字内容
            String feedbackText;
            if (text != null && !text.isBlank()) {
                feedbackText = text.trim();
            } else {
                feedbackText = aiService.generateFeedbackText(type, subject);
            }

            // 2. 选择 TTS 引擎
            String engine = audioProperties.getEngine();
            QuestionAudioGenerator generator = generators.get(engine);
            if (generator == null) {
                return R.fail("TTS 引擎不可用: " + engine);
            }

            // 3. 生成音频
            Path audioFile = generator.generate(feedbackText);

            // 4. 上传到 FTP
            String serviceDir = "/question/audio/feedback";
            String fileName = FILE_TIME.format(LocalDateTime.now()) + "-" + type + ".wav";
            try (InputStream is = Files.newInputStream(audioFile)) {
                fileName = ftpTool.upload(serviceDir, fileName, is);
            }

            // 5. 构建公开 URL
            String audioUrl = ftpTool.buildPublicUrl(serviceDir, fileName);

            // 6. 清理临时文件
            try { Files.deleteIfExists(audioFile); } catch (Exception ignored) {}

            GenerateResult result = new GenerateResult();
            result.setText(feedbackText);
            result.setAudioUrl(audioUrl);
            return R.ok(result);

        } catch (BusinessException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("Feedback audio generation failed", e);
            return R.fail("生成失败: " + e.getMessage());
        }
    }

    // ===== App 端接口：供小程序调用 =====

    @Operation(summary = "获取反馈语音配置（App端）")
    @GetMapping("/public/config")
    public R<FeedbackAudioConfig> publicDetail() {
        return detail();
    }

    // 公开接口，无需登录
    @Tag(name = "公开-反馈语音配置")
    @RestController
    @RequestMapping("/api/v1/public/feedback-audio")
    @RequiredArgsConstructor
    public static class PublicFeedbackAudioController {
        private final AppConfigMapper appConfigMapper;

        @Operation(summary = "获取反馈语音配置")
        @GetMapping("/config")
        public R<FeedbackAudioConfig> detail() {
            String prefix = "feedback.audio.";
            String defaultBaseUrl = "https://ftp.pnkx.top:8/ftp/kids-learn/question/audio/seed/feedback";
            String defaultCorrectList = "correct-1,correct-2,correct-3,correct-4,correct-5";
            String defaultWrongList = "wrong-1,wrong-2,wrong-3";

            List<AppConfig> configs = appConfigMapper.selectList(
                new LambdaQueryWrapper<AppConfig>()
                    .likeRight(AppConfig::getConfigKey, prefix)
            );
            Map<String, AppConfig> map = new LinkedHashMap<>();
            for (AppConfig config : configs) {
                if (config.getConfigKey() != null) {
                    map.put(config.getConfigKey(), config);
                }
            }

            FeedbackAudioConfig response = new FeedbackAudioConfig();
            response.setBaseUrl(val(map, prefix + "base_url", defaultBaseUrl));
            response.setCorrectList(val(map, prefix + "correct_list", defaultCorrectList));
            response.setWrongList(val(map, prefix + "wrong_list", defaultWrongList));
            return R.ok(response);
        }

        private String val(Map<String, AppConfig> configs, String key, String defaultValue) {
            AppConfig config = configs.get(key);
            if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) return defaultValue;
            return config.getConfigValue().trim();
        }
    }

    // ===== 工具方法 =====

    private Map<String, AppConfig> loadConfigs() {
        List<AppConfig> configs = appConfigMapper.selectList(
            new LambdaQueryWrapper<AppConfig>()
                .likeRight(AppConfig::getConfigKey, PREFIX)
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
        if (config.getDescription() == null || config.getDescription().isBlank()) {
            config.setDescription(description);
        }
        if (config.getConfigType() == null) config.setConfigType(1);
        appConfigMapper.updateById(config);
    }

    private static String val(Map<String, AppConfig> configs, String key, String defaultValue) {
        AppConfig config = configs.get(key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) return defaultValue;
        return config.getConfigValue().trim();
    }

    private static int parseInt(String value, int defaultValue) {
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return defaultValue; }
    }

    @Data
    public static class FeedbackAudioConfig {
        private String baseUrl;
        private String correctList;
        private String wrongList;
    }

    @Data
    public static class GenerateRequest {
        private String type;      // "correct" 或 "wrong"
        private String subject;   // 学科，可选
        private String text;      // 自定义文字，可选（不传则 AI 生成）
    }

    @Data
    public static class GenerateResult {
        private String text;      // 生成的文字
        private String audioUrl;  // 音频 URL
    }
}
