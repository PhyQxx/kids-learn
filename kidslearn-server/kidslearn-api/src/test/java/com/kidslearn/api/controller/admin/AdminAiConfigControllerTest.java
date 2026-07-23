package com.kidslearn.api.controller.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kidslearn.api.entity.AppConfig;
import com.kidslearn.api.mapper.AppConfigMapper;
import com.kidslearn.api.service.AiService;
import com.kidslearn.api.service.impl.AdminOperationLogService;
import com.kidslearn.api.service.impl.QuestionAudioProperties;
import com.kidslearn.common.result.R;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminAiConfigControllerTest {

    @Test
    void listsProvidersWithoutReturningApiKeys() {
        AppConfigMapper mapper = mock(AppConfigMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
            config(1L, "ai.provider", "deepseek"),
            config(2L, "ai.deepseek.api_key", "secret-key"),
            config(3L, "ai.deepseek.base_url", "https://api.deepseek.com"),
            config(4L, "ai.deepseek.model", "deepseek-chat"),
            config(5L, "ai.timeout", "18")
        ));
        AdminAiConfigController controller = new AdminAiConfigController(
            mapper,
            mock(AdminOperationLogService.class),
            mock(AiService.class),
            mock(QuestionAudioProperties.class)
        );

        R<AdminAiConfigController.AiConfigResponse> result = controller.detail();

        assertEquals(200, result.getCode());
        assertEquals("deepseek", result.getData().getProvider());
        assertEquals(18, result.getData().getTimeout());
        AdminAiConfigController.ProviderConfig deepseek = result.getData().getCategories().stream()
            .flatMap(category -> category.getProviders().stream())
            .filter(item -> "deepseek".equals(item.getProvider()))
            .findFirst()
            .orElseThrow();
        assertTrue(deepseek.isApiKeyConfigured());
        assertEquals("", deepseek.getApiKey());
        assertEquals("https://api.deepseek.com", deepseek.getBaseUrl());
        assertEquals("deepseek-chat", deepseek.getModel());
    }

    @Test
    void savesActiveProviderAndProviderSettingsToDatabase() {
        AppConfigMapper mapper = mock(AppConfigMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
            config(1L, "ai.provider", "deepseek"),
            config(2L, "ai.timeout", "15")
        ));
        AdminOperationLogService logService = mock(AdminOperationLogService.class);
        AiService aiService = mock(AiService.class);
        AdminAiConfigController controller = new AdminAiConfigController(
            mapper, logService, aiService, mock(QuestionAudioProperties.class));
        AdminAiConfigController.AiConfigRequest request = new AdminAiConfigController.AiConfigRequest();
        request.setProvider("openai");
        request.setTimeout(30);
        request.setProviders(List.of(provider("openai", true, "https://api.openai.com", "gpt-4o-mini", "sk-new")));

        R<Void> result = controller.save(request);

        assertEquals(200, result.getCode());
        ArgumentCaptor<AppConfig> updateCaptor = ArgumentCaptor.forClass(AppConfig.class);
        verify(mapper, org.mockito.Mockito.atLeastOnce()).updateById(updateCaptor.capture());
        assertTrue(updateCaptor.getAllValues().stream()
            .anyMatch(item -> "ai.provider".equals(item.getConfigKey()) && "openai".equals(item.getConfigValue())));
        assertTrue(updateCaptor.getAllValues().stream()
            .anyMatch(item -> "ai.timeout".equals(item.getConfigKey()) && "30".equals(item.getConfigValue())));

        ArgumentCaptor<AppConfig> insertCaptor = ArgumentCaptor.forClass(AppConfig.class);
        verify(mapper, org.mockito.Mockito.atLeastOnce()).insert(insertCaptor.capture());
        assertTrue(insertCaptor.getAllValues().stream()
            .anyMatch(item -> "ai.openai.api_key".equals(item.getConfigKey()) && "sk-new".equals(item.getConfigValue())));
        assertTrue(insertCaptor.getAllValues().stream()
            .anyMatch(item -> "ai.openai.base_url".equals(item.getConfigKey()) && "https://api.openai.com".equals(item.getConfigValue())));
        assertTrue(insertCaptor.getAllValues().stream()
            .anyMatch(item -> "ai.openai.model".equals(item.getConfigKey()) && "gpt-4o-mini".equals(item.getConfigValue())));
        verify(logService).write("ai-config", "save", "app-config", null, "saved");
        verify(aiService).clearCache();
    }

    @Test
    void blankApiKeyDoesNotOverwriteStoredSecret() {
        AppConfigMapper mapper = mock(AppConfigMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
            config(1L, "ai.provider", "deepseek"),
            config(2L, "ai.deepseek.api_key", "old-secret")
        ));
        AdminAiConfigController controller = new AdminAiConfigController(
            mapper,
            mock(AdminOperationLogService.class),
            mock(AiService.class),
            mock(QuestionAudioProperties.class)
        );
        AdminAiConfigController.AiConfigRequest request = new AdminAiConfigController.AiConfigRequest();
        request.setProvider("deepseek");
        request.setTimeout(15);
        request.setProviders(List.of(provider("deepseek", true, "https://api.deepseek.com", "deepseek-chat", "")));

        R<Void> result = controller.save(request);

        assertEquals(200, result.getCode());
        ArgumentCaptor<AppConfig> updateCaptor = ArgumentCaptor.forClass(AppConfig.class);
        verify(mapper, org.mockito.Mockito.atLeastOnce()).updateById(updateCaptor.capture());
        assertFalse(updateCaptor.getAllValues().stream()
            .anyMatch(item -> "ai.deepseek.api_key".equals(item.getConfigKey())));
    }

    private static AppConfig config(Long id, String key, String value) {
        AppConfig config = new AppConfig();
        config.setId(id);
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }

    private static AdminAiConfigController.ProviderConfig provider(
        String provider,
        boolean enabled,
        String baseUrl,
        String model,
        String apiKey
    ) {
        AdminAiConfigController.ProviderConfig config = new AdminAiConfigController.ProviderConfig();
        config.setProvider(provider);
        config.setEnabled(enabled);
        config.setBaseUrl(baseUrl);
        config.setModel(model);
        config.setApiKey(apiKey);
        return config;
    }
}
