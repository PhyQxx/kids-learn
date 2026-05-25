package com.kidslearn.api.controller.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidslearn.api.entity.AppConfig;
import com.kidslearn.api.entity.OperationLog;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.AdminRoleMapper;
import com.kidslearn.api.mapper.AppConfigMapper;
import com.kidslearn.api.mapper.AppVersionMapper;
import com.kidslearn.api.mapper.OperationLogMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.api.service.AdminDashboardStatsService;
import com.kidslearn.api.service.impl.AdminOperationLogService;
import com.kidslearn.api.service.impl.PasswordHashService;
import com.kidslearn.common.result.PageResult;
import com.kidslearn.common.result.R;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminSystemControllerTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final OperationLogMapper operationLogMapper = mock(OperationLogMapper.class);
    private final AdminSystemController controller = new AdminSystemController(
        userMapper,
        mock(AdminRoleMapper.class),
        mock(AppConfigMapper.class),
        operationLogMapper,
        mock(AppVersionMapper.class),
        mock(AdminDashboardStatsService.class),
        new PasswordHashService(),
        new AdminOperationLogService(operationLogMapper)
    );

    @Test
    void rejectsInvalidUserStatus() {
        R<Void> result = controller.userStatus(7L, 2);

        assertEquals(500, result.getCode());
        verify(userMapper, never()).updateById(org.mockito.ArgumentMatchers.any());
        verify(operationLogMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void writesOperationLogWhenUserStatusChanges() {
        User user = new User();
        user.setId(7L);
        user.setUsername("demo");
        user.setStatus(1);
        when(userMapper.selectById(7L)).thenReturn(user);

        R<Void> result = controller.userStatus(7L, 0);

        assertEquals(200, result.getCode());
        assertEquals(0, user.getStatus());
        verify(userMapper).updateById(user);
        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogMapper).insert(captor.capture());
        assertEquals("system-user", captor.getValue().getModule());
        assertEquals("change-status", captor.getValue().getAction());
        assertEquals("user", captor.getValue().getTargetType());
        assertEquals(7L, captor.getValue().getTargetId());
    }

    @Test
    void rejectsCreateUserWithoutUsernameOrPassword() {
        R<Void> result = controller.userSave(Map.of("nickname", "Demo"));

        assertEquals(500, result.getCode());
        verify(userMapper, never()).insert(org.mockito.ArgumentMatchers.any());
        verify(operationLogMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void excludesAiConfigFromGeneralConfigList() {
        AppConfigMapper appConfigMapper = mock(AppConfigMapper.class);
        AdminSystemController controller = controllerWithAppConfigMapper(appConfigMapper);
        Page<AppConfig> page = new Page<AppConfig>(1, 20);
        page.setRecords(List.of(
            config("ai.deepseek.api_key", "secret"),
            config("site.name", "KidsLearn")
        ));
        page.setTotal(2);
        when(appConfigMapper.selectPage(any(), any())).thenReturn(page);

        R<PageResult<AppConfig>> result = controller.configList(1, 20);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getList().size());
        assertEquals("site.name", result.getData().getList().get(0).getConfigKey());
    }

    @Test
    void rejectsAiConfigSaveFromGeneralConfigEndpoint() {
        AppConfigMapper appConfigMapper = mock(AppConfigMapper.class);
        AdminSystemController controller = controllerWithAppConfigMapper(appConfigMapper);
        AppConfig config = new AppConfig();
        config.setId(1L);
        config.setConfigKey("ai.deepseek.api_key");
        config.setConfigValue("secret");

        R<Void> result = controller.configSave(config);

        assertEquals(500, result.getCode());
        verify(appConfigMapper, never()).updateById(any());
    }

    private AdminSystemController controllerWithAppConfigMapper(AppConfigMapper appConfigMapper) {
        return new AdminSystemController(
            userMapper,
            mock(AdminRoleMapper.class),
            appConfigMapper,
            operationLogMapper,
            mock(AppVersionMapper.class),
            mock(AdminDashboardStatsService.class),
            new PasswordHashService(),
            new AdminOperationLogService(operationLogMapper)
        );
    }

    private static AppConfig config(String key, String value) {
        AppConfig config = new AppConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }
}
