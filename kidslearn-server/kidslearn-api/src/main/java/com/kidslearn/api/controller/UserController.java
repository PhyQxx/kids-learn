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
}
