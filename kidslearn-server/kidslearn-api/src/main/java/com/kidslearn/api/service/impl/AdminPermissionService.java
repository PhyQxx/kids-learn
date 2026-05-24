package com.kidslearn.api.service.impl;

import com.kidslearn.api.entity.AdminRole;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.AdminRoleMapper;
import com.kidslearn.api.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPermissionService {

    private static final String ADMIN_PREFIX = "/api/v1/admin/";

    private final UserMapper userMapper;
    private final AdminRoleMapper adminRoleMapper;

    public boolean hasPermission(Long userId, String method, String requestUri) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getUserType() == null || user.getUserType() != 3) {
            return false;
        }
        if (user.getRoleId() == null) {
            return true;
        }

        AdminRole role = adminRoleMapper.selectById(user.getRoleId());
        if (role == null || role.getPermissions() == null || role.getPermissions().isBlank()) {
            return false;
        }

        Set<String> permissions = Arrays.stream(role.getPermissions().split("[,;\\s]+"))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toSet());
        String module = resolveModule(requestUri);
        String action = resolveAction(method);

        return permissions.contains("*")
            || permissions.contains("admin:*")
            || permissions.contains("admin:" + module + ":*")
            || permissions.contains("admin:" + module + ":" + action);
    }

    private String resolveModule(String requestUri) {
        if (requestUri == null || !requestUri.startsWith(ADMIN_PREFIX)) {
            return "unknown";
        }
        String rest = requestUri.substring(ADMIN_PREFIX.length());
        int slashIndex = rest.indexOf('/');
        return slashIndex >= 0 ? rest.substring(0, slashIndex) : rest;
    }

    private String resolveAction(String method) {
        if ("GET".equalsIgnoreCase(method)) {
            return "read";
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            return "delete";
        }
        return "write";
    }
}
