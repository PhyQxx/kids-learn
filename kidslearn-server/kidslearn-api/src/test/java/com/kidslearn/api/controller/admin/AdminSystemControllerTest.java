package com.kidslearn.api.controller.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.kidslearn.common.result.R;
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
}
