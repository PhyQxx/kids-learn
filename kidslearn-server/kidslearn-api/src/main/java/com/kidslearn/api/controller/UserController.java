package com.kidslearn.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.dto.user.UpdateChildProfileDTO;
import com.kidslearn.api.dto.user.UserVO;
import com.kidslearn.api.entity.AdminRole;
import com.kidslearn.api.entity.ChildProfile;
import com.kidslearn.api.entity.GradeLevel;
import com.kidslearn.api.entity.ParentProfile;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.AdminRoleMapper;
import com.kidslearn.api.mapper.ChildProfileMapper;
import com.kidslearn.api.mapper.GradeLevelMapper;
import com.kidslearn.api.mapper.ParentProfileMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.api.service.AuthService;
import com.kidslearn.api.service.impl.ParentPinService;
import com.kidslearn.api.service.impl.AccountSecurityService;
import com.kidslearn.api.service.impl.SmsVerificationService;
import com.kidslearn.api.service.impl.AccountDataExportService;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Tag(name = "用户接口")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final ChildProfileMapper childProfileMapper;
    private final ParentProfileMapper parentProfileMapper;
    private final GradeLevelMapper gradeLevelMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final AuthService authService;
    private final ParentPinService parentPinService;
    private final AccountSecurityService accountSecurityService;
    private final SmsVerificationService smsVerificationService;
    private final AccountDataExportService accountDataExportService;

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public R<UserVO> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userMapper.selectById(userId);
        if (user == null) {
            return R.fail("用户不存在");
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setUserId(user.getId());

        // 管理员填充权限列表
        if (user.getUserType() != null && user.getUserType() == 3) {
            vo.setPermissions(resolveAdminPermissions(user));
        }

        // populate gradeLevel from child profile
        ChildProfile childProfile = childProfileMapper.selectOne(
            new LambdaQueryWrapper<ChildProfile>().eq(ChildProfile::getUserId, userId)
        );
        if (childProfile != null && childProfile.getGradeLevel() != null) {
            GradeLevel gl = gradeLevelMapper.selectById(childProfile.getGradeLevel());
            if (gl != null) {
                vo.setGradeLevelId(gl.getId());
                vo.setGradeLevelName(gl.getLevelName());
            }
        }
        // populate phone from parent profile
        ParentProfile parentProfile = parentProfileMapper.selectOne(
            new LambdaQueryWrapper<ParentProfile>().eq(ParentProfile::getUserId, userId)
        );
        if (parentProfile != null) {
            vo.setPhone(parentProfile.getPhone());
        }
        return R.ok(vo);
    }

    @Operation(summary = "更新用户基础资料")
    @PutMapping("/info")
    public R<Void> updateUserInfo(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userMapper.selectById(userId);
        if (user == null) return R.fail("用户不存在");
        if (body.containsKey("nickname")) {
            String nickname = body.get("nickname") == null ? "" : body.get("nickname").trim();
            if (nickname.isBlank() || nickname.length() > 20) return R.fail("昵称长度必须为1-20位");
            user.setNickname(nickname);
        }
        if (body.containsKey("avatar")) user.setAvatar(body.get("avatar"));
        userMapper.updateById(user);
        return R.ok();
    }

    /**
     * 解析管理员权限码列表
     */
    private List<String> resolveAdminPermissions(User user) {
        // 未分配角色 = 超级管理员，拥有所有权限
        if (user.getRoleId() == null) {
            return List.of("admin:*");
        }
        AdminRole role = adminRoleMapper.selectById(user.getRoleId());
        if (role == null || role.getPermissions() == null || role.getPermissions().isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(role.getPermissions().split("[,;\\s]+"))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }

    @Operation(summary = "更新学习档案")
    @PutMapping("/child-profile")
    public R<Void> updateChildProfile(HttpServletRequest request, @Valid @RequestBody UpdateChildProfileDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        ChildProfile profile = childProfileMapper.selectOne(
            new LambdaQueryWrapper<ChildProfile>().eq(ChildProfile::getUserId, userId)
        );
        // auto-create child_profile for old accounts that don't have one
        if (profile == null) {
            profile = new ChildProfile();
            profile.setUserId(userId);
            profile.setGender(0);
            profile.setLearnAgeGroup(dto.getLearnAgeGroup() != null ? dto.getLearnAgeGroup() : 2);
            profile.setGradeLevel(dto.getGradeLevel() != null ? dto.getGradeLevel() : 4);
            childProfileMapper.insert(profile);
            return R.ok();
        }
        if (dto.getLearnAgeGroup() != null) {
            profile.setLearnAgeGroup(dto.getLearnAgeGroup());
        }
        if (dto.getGradeLevel() != null) {
            profile.setGradeLevel(dto.getGradeLevel());
        }
        if (dto.getGender() != null) {
            profile.setGender(dto.getGender());
        }
        if (dto.getBirthDate() != null) {
            profile.setBirthDate(LocalDate.parse(dto.getBirthDate()));
        }
        if (dto.getSchoolName() != null) {
            profile.setSchoolName(dto.getSchoolName());
        }
        if (dto.getClassName() != null) {
            profile.setClassName(dto.getClassName());
        }
        childProfileMapper.updateById(profile);
        return R.ok();
    }

    @Operation(summary = "更新新手引导步骤")
    @PostMapping("/onboarding-step")
    public R<Void> updateOnboardingStep(HttpServletRequest request, @RequestParam Integer step) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setOnboardingStep(step);
            userMapper.updateById(user);
        }
        return R.ok();
    }

    @Operation(summary = "验证当前账号密码")
    @PostMapping("/verify-password")
    public R<Void> verifyPassword(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        authService.verifyPassword(userId, body.get("password"));
        return R.ok();
    }

    @Operation(summary = "获取家长PIN状态")
    @GetMapping("/parent-pin/status")
    public R<Map<String, Object>> getParentPinStatus(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(parentPinService.status(userId));
    }

    @Operation(summary = "为存量账号设置家长PIN")
    @PostMapping("/parent-pin/setup")
    public R<Void> setupParentPin(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        parentPinService.setup(userId, body.get("password"), body.get("pin"));
        return R.ok();
    }

    @Operation(summary = "验证家长PIN")
    @PostMapping("/parent-pin/verify")
    public R<Void> verifyParentPin(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        parentPinService.verify(userId, body.get("pin"));
        return R.ok();
    }

    @Operation(summary = "修改家长PIN")
    @PutMapping("/parent-pin")
    public R<Void> changeParentPin(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        parentPinService.change(userId, body.get("currentPin"), body.get("newPin"));
        return R.ok();
    }

    @Operation(summary = "修改账号密码")
    @PutMapping("/password")
    public R<Void> changePassword(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        accountSecurityService.changePassword(
            userId, body.get("oldPassword"), body.get("newPassword"), (String) request.getAttribute("token"));
        return R.ok();
    }

    @Operation(summary = "发送换绑手机号验证码")
    @PostMapping("/phone/send-code")
    public R<Map<String, Object>> sendPhoneChangeCode(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return R.ok(smsVerificationService.send(
            body.get("phone"), SmsVerificationService.Purpose.PHONE_CHANGE, clientIp(request)));
    }

    @Operation(summary = "换绑手机号")
    @PutMapping("/phone")
    public R<Void> changePhone(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        accountSecurityService.changePhone(userId, body.get("password"), body.get("phone"), body.get("code"));
        return R.ok();
    }

    @Operation(summary = "获取登录设备")
    @GetMapping("/devices")
    public R<List<Map<String, Object>>> listDevices(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(accountSecurityService.listDevices(userId, (String) request.getAttribute("token")));
    }

    @Operation(summary = "远程退出登录设备")
    @DeleteMapping("/devices/{deviceId}")
    public R<Void> revokeDevice(HttpServletRequest request, @PathVariable String deviceId) {
        Long userId = (Long) request.getAttribute("userId");
        accountSecurityService.revokeDevice(userId, deviceId, (String) request.getAttribute("token"));
        return R.ok();
    }

    @Operation(summary = "停用当前账号")
    @PostMapping("/account-cancellation")
    public R<Void> deactivateAccount(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        accountSecurityService.deactivateAccount(userId, body.get("password"), body.get("parentPin"));
        return R.ok();
    }

    @Operation(summary = "导出当前账号数据")
    @PostMapping("/data-export")
    public R<Map<String, Object>> exportData(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return R.ok(accountDataExportService.export((Long) request.getAttribute("userId"), body.get("parentPin")));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
