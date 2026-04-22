package com.kidslearn.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.dto.user.UpdateChildProfileDTO;
import com.kidslearn.api.dto.user.UserVO;
import com.kidslearn.api.entity.ChildProfile;
import com.kidslearn.api.entity.GradeLevel;
import com.kidslearn.api.entity.ParentProfile;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.ChildProfileMapper;
import com.kidslearn.api.mapper.GradeLevelMapper;
import com.kidslearn.api.mapper.ParentProfileMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "用户接口")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final ChildProfileMapper childProfileMapper;
    private final ParentProfileMapper parentProfileMapper;
    private final GradeLevelMapper gradeLevelMapper;

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

    @Operation(summary = "更新学习档案")
    @PutMapping("/child-profile")
    public R<Void> updateChildProfile(HttpServletRequest request, @RequestBody UpdateChildProfileDTO dto) {
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
}
