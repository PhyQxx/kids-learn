package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidslearn.api.entity.*;
import com.kidslearn.api.mapper.*;
import com.kidslearn.api.service.AdminDashboardStatsService;
import com.kidslearn.api.service.impl.AdminOperationLogService;
import com.kidslearn.api.service.impl.PasswordHashService;
import com.kidslearn.common.result.PageResult;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理后台-系统管理")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminSystemController {

    private final UserMapper userMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final AppConfigMapper appConfigMapper;
    private final OperationLogMapper operationLogMapper;
    private final AppVersionMapper appVersionMapper;
    private final AdminDashboardStatsService adminDashboardStatsService;
    private final PasswordHashService passwordHashService;
    private final AdminOperationLogService adminOperationLogService;

    // ==================== Dashboard ====================

    @Operation(summary = "首页统计")
    @GetMapping("/dashboard/stats")
    public R<Map<String, Object>> dashboardStats() {
        return R.ok(adminDashboardStatsService.getStats());
    }

    // ==================== 用户管理 ====================

    @GetMapping("/user/list")
    public R<PageResult<User>> userList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer userType) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getNickname, keyword).or().like(User::getUsername, keyword));
        }
        if (userType != null) {
            if (userType == 3) {
                wrapper.eq(User::getUserType, 3);
            } else {
                wrapper.in(User::getUserType, 1, 2);
            }
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> p = userMapper.selectPage(new Page<>(page, pageSize), wrapper);
        p.getRecords().forEach(user -> user.setUserType(normalizeDisplayUserType(user.getUserType())));
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @PostMapping("/user/{id}/status")
    public R<Void> userStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (id == null || id <= 0) {
            return R.fail("用户ID无效");
        }
        if (status == null || (status != 0 && status != 1)) {
            return R.fail("状态值无效");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            return R.fail("用户不存在");
        }

        Integer oldStatus = user.getStatus();
        user.setStatus(status);
        userMapper.updateById(user);
        adminOperationLogService.write("system-user", "change-status", "user", id,
            "username=" + safe(user.getUsername()) + ", status=" + oldStatus + "->" + status);
        return R.ok();
    }

    @Operation(summary = "新增/编辑用户")
    @PostMapping("/user/save")
    public R<Void> userSave(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") != null ? Long.valueOf(body.get("id").toString()) : null;
        if (id == null) {
            // 新增
            String username = safe(body.get("username"));
            String password = safe(body.get("password"));
            if (username.isBlank()) {
                return R.fail("用户名不能为空");
            }
            if (password.isBlank()) {
                return R.fail("密码不能为空");
            }
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            if (count > 0) return R.fail("用户名已存在");

            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordHashService.hash(password));
            user.setNickname(body.getOrDefault("nickname", "").toString());
            user.setUserType(normalizePersistedUserType(body.getOrDefault("userType", "1")));
            user.setStatus(Integer.valueOf(body.getOrDefault("status", "1").toString()));
            user.setRealName(body.get("realName") != null ? body.get("realName").toString() : null);
            user.setRoleId(body.get("roleId") != null ? Long.valueOf(body.get("roleId").toString()) : null);
            user.setTotalExp(0);
            user.setLevel(1);
            user.setGold(0);
            user.setDiamond(0);
            userMapper.insert(user);
            adminOperationLogService.write("system-user", "create", "user", user.getId(),
                "username=" + username + ", userType=" + user.getUserType());
        } else {
            // 编辑
            if (id <= 0) {
                return R.fail("用户ID无效");
            }
            User user = userMapper.selectById(id);
            if (user == null) return R.fail("用户不存在");
            if (body.containsKey("nickname")) user.setNickname(body.get("nickname").toString());
            if (body.containsKey("status")) user.setStatus(Integer.valueOf(body.get("status").toString()));
            if (body.containsKey("userType")) user.setUserType(normalizePersistedUserType(body.get("userType")));
            if (body.containsKey("realName")) user.setRealName(body.get("realName").toString());
            if (body.containsKey("roleId")) user.setRoleId(body.get("roleId") != null ? Long.valueOf(body.get("roleId").toString()) : null);
            if (body.containsKey("password") && !body.get("password").toString().isEmpty()) {
                user.setPassword(passwordHashService.hash(body.get("password").toString()));
            }
            userMapper.updateById(user);
            adminOperationLogService.write("system-user", "update", "user", id,
                "username=" + safe(user.getUsername()) + ", userType=" + user.getUserType());
        }
        return R.ok();
    }

    private static String safe(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private static int normalizePersistedUserType(Object value) {
        if (value != null && Integer.parseInt(value.toString()) == 3) {
            return 3;
        }
        return 1;
    }

    private static int normalizeDisplayUserType(Integer userType) {
        return userType != null && userType == 3 ? 3 : 1;
    }

    // ==================== 管理员管理 ====================

    @GetMapping("/admin-user/list")
    public R<PageResult<User>> adminUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<User> p = userMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<User>().eq(User::getUserType, 3).orderByDesc(User::getCreateTime));
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    // ==================== 角色管理 ====================

    @GetMapping("/role/list")
    public R<List<AdminRole>> roleList() {
        return R.ok(adminRoleMapper.selectList(new LambdaQueryWrapper<AdminRole>().orderByAsc(AdminRole::getId)));
    }

    @PostMapping("/role/save")
    public R<Void> roleSave(@RequestBody AdminRole role) {
        if (role.getId() == null) adminRoleMapper.insert(role);
        else adminRoleMapper.updateById(role);
        return R.ok();
    }

    @DeleteMapping("/role/{id}")
    public R<Void> roleDelete(@PathVariable Long id) {
        adminRoleMapper.deleteById(id);
        adminOperationLogService.write("system-role", "delete", "role", id, "delete role");
        return R.ok();
    }

    // ==================== 系统配置 ====================

    @GetMapping("/config/list")
    public R<PageResult<AppConfig>> configList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<AppConfig> p = appConfigMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<AppConfig>().orderByAsc(AppConfig::getId));
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @PostMapping("/config/save")
    public R<Void> configSave(@RequestBody AppConfig config) {
        appConfigMapper.updateById(config);
        return R.ok();
    }

    // ==================== 操作日志 ====================

    @GetMapping("/log/list")
    public R<PageResult<OperationLog>> logList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String module) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
            .eq(module != null && !module.isEmpty(), OperationLog::getModule, module)
            .orderByDesc(OperationLog::getCreateTime);
        Page<OperationLog> p = operationLogMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    // ==================== 版本管理 ====================

    @GetMapping("/version/list")
    public R<PageResult<AppVersion>> versionList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<AppVersion> p = appVersionMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<AppVersion>().orderByDesc(AppVersion::getVersionCode));
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @PostMapping("/version/save")
    public R<Void> versionSave(@RequestBody AppVersion version) {
        if (version.getId() == null) {
            appVersionMapper.insert(version);
        } else {
            appVersionMapper.updateById(version);
        }
        return R.ok();
    }

    @DeleteMapping("/version/{id}")
    public R<Void> versionDelete(@PathVariable Long id) {
        appVersionMapper.deleteById(id);
        adminOperationLogService.write("system-version", "delete", "app-version", id, "delete app version");
        return R.ok();
    }
}
