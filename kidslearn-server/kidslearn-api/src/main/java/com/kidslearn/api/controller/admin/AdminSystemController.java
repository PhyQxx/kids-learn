package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidslearn.api.entity.*;
import com.kidslearn.api.mapper.*;
import com.kidslearn.common.result.PageResult;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "管理后台-系统管理")
@RestController
@RequestMapping("/admin/api/v1")
@RequiredArgsConstructor
public class AdminSystemController {

    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final AppConfigMapper appConfigMapper;
    private final OperationLogMapper operationLogMapper;
    private final CourseMapper courseMapper;
    private final OrderMapper orderMapper;

    // ==================== Dashboard ====================

    @Operation(summary = "首页统计")
    @GetMapping("/dashboard/stats")
    public R<Map<String, Object>> dashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUserType, 1)));
        stats.put("todayActiveUsers", 0L); // placeholder, requires Redis tracking
        stats.put("totalCourses", courseMapper.selectCount(new LambdaQueryWrapper<>()));
        stats.put("totalOrders", orderMapper.selectCount(new LambdaQueryWrapper<>()));
        stats.put("todayRevenue", 0L);
        return R.ok(stats);
    }

    // ==================== 用户管理 ====================

    @GetMapping("/user/list")
    public R<PageResult<User>> userList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer userType) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
            .like(keyword != null && !keyword.isEmpty(), User::getNickname, keyword)
            .or()
            .like(keyword != null && !keyword.isEmpty(), User::getUsername, keyword)
            .eq(userType != null, User::getUserType, userType)
            .orderByDesc(User::getCreateTime);
        Page<User> p = userMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @PostMapping("/user/{id}/status")
    public R<Void> userStatus(@PathVariable Long id, @RequestParam Integer status) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setStatus(status);
            userMapper.updateById(user);
        }
        return R.ok();
    }

    // ==================== 管理员管理 ====================

    @GetMapping("/admin-user/list")
    public R<PageResult<AdminUser>> adminUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<AdminUser> p = adminUserMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<AdminUser>().orderByDesc(AdminUser::getCreateTime));
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
}
