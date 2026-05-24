package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kidslearn.api.entity.AdminRole;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.AdminRoleMapper;
import com.kidslearn.api.mapper.UserMapper;
import org.junit.jupiter.api.Test;

class AdminPermissionServiceTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final AdminRoleMapper adminRoleMapper = mock(AdminRoleMapper.class);
    private final AdminPermissionService service = new AdminPermissionService(userMapper, adminRoleMapper);

    @Test
    void adminWithoutRoleIsSuperAdminForCompatibility() {
        User user = adminUser(null);
        when(userMapper.selectById(1L)).thenReturn(user);

        assertTrue(service.hasPermission(1L, "DELETE", "/api/v1/admin/question/9"));
    }

    @Test
    void rolePermissionsControlAdminActions() {
        User user = adminUser(2L);
        AdminRole role = new AdminRole();
        role.setPermissions("admin:question:read,admin:subject:*");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(adminRoleMapper.selectById(2L)).thenReturn(role);

        assertTrue(service.hasPermission(1L, "GET", "/api/v1/admin/question/list"));
        assertTrue(service.hasPermission(1L, "DELETE", "/api/v1/admin/subject/3"));
        assertFalse(service.hasPermission(1L, "DELETE", "/api/v1/admin/question/9"));
    }

    @Test
    void wildcardPermissionAllowsEveryAdminAction() {
        User user = adminUser(2L);
        AdminRole role = new AdminRole();
        role.setPermissions("admin:*");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(adminRoleMapper.selectById(2L)).thenReturn(role);

        assertTrue(service.hasPermission(1L, "DELETE", "/api/v1/admin/version/9"));
    }

    private User adminUser(Long roleId) {
        User user = new User();
        user.setId(1L);
        user.setUserType(3);
        user.setRoleId(roleId);
        return user;
    }
}
