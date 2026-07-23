package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.dto.auth.LoginDTO;
import com.kidslearn.api.dto.auth.RegisterDTO;
import com.kidslearn.api.dto.auth.TokenVO;
import com.kidslearn.api.dto.user.UserVO;
import com.kidslearn.api.entity.*;
import com.kidslearn.api.mapper.*;
import com.kidslearn.api.service.AuthService;
import com.kidslearn.common.constants.RedisConstants;
import com.kidslearn.common.exception.BusinessException;
import com.kidslearn.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final ChildProfileMapper childProfileMapper;
    private final ParentProfileMapper parentProfileMapper;
    private final GradeLevelMapper gradeLevelMapper;
    private final UserLoginLogMapper userLoginLogMapper;
    private final PetMapper petMapper;
    private final UserPetMapper userPetMapper;
    private final com.kidslearn.api.mapper.AdminRoleMapper adminRoleMapper;
    private final StringRedisTemplate redisTemplate;
    private final PasswordHashService passwordHashService;
    private final SmsVerificationService smsVerificationService;

    @Override
    public TokenVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        String rawPassword = dto.getPassword();
        if (!passwordHashService.matches(rawPassword, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }

        // update login time
        if (passwordHashService.needsUpgrade(user.getPassword())) {
            user.setPassword(passwordHashService.hash(rawPassword));
        }
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // log login (not for admin)
        if (user.getUserType() != 3) {
            UserLoginLog log = new UserLoginLog();
            log.setUserId(user.getId());
            log.setLoginType(dto.getLoginType() != null ? dto.getLoginType() : user.getUserType());
            log.setLoginTime(LocalDateTime.now());
            userLoginLogMapper.insert(log);
        }

        return buildToken(user);
    }

    @Override
    @Transactional
    public TokenVO register(RegisterDTO dto) {
        if (dto.getParentPin() == null || !dto.getParentPin().matches("^\\d{6}$")) {
            throw new BusinessException("家长PIN必须是6位数字");
        }
        // check username unique
        Long count = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        String phone = SmsVerificationService.normalizePhone(dto.getPhone());
        Long phoneCount = parentProfileMapper.selectCount(
            new LambdaQueryWrapper<ParentProfile>().eq(ParentProfile::getPhone, phone)
        );
        if (phoneCount > 0) {
            throw new BusinessException("该手机号已绑定其他账号");
        }
        if (!Boolean.TRUE.equals(dto.getGuardianConsent())) {
            throw new BusinessException("需要监护人同意后才能注册");
        }
        smsVerificationService.verifyAndConsume(phone, SmsVerificationService.Purpose.REGISTER, dto.getVerifyCode());

        // admin registration not allowed
        if (dto.getUserType() != null && dto.getUserType() == 3) {
            throw new BusinessException("不允许注册管理员账号");
        }

        // create user (always family account)
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordHashService.hash(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setUserType(1);
        user.setStatus(1);
        user.setTotalExp(0);
        user.setLevel(1);
        user.setGold(0);
        user.setDiamond(0);
        userMapper.insert(user);

        // always create child profile (learning settings)
        Integer ageGroup = dto.getLearnAgeGroup() != null ? dto.getLearnAgeGroup() : 2;
        ChildProfile childProfile = new ChildProfile();
        childProfile.setUserId(user.getId());
        childProfile.setLearnAgeGroup(ageGroup);
        if (dto.getBirthDate() != null) {
            try {
                childProfile.setBirthDate(LocalDate.parse(dto.getBirthDate(), DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (Exception e) {
                // Ignore date parsing error
            }
        }
        childProfile.setGender(dto.getGender() != null ? dto.getGender() : 0);
        if (dto.getGradeLevel() != null) {
            childProfile.setGradeLevel(dto.getGradeLevel());
        } else {
            childProfile.setGradeLevel(switch (ageGroup) {
                case 1 -> 1;
                case 3 -> 7;
                default -> 4;
            });
        }
        childProfileMapper.insert(childProfile);

        // always create parent profile (contact info)
        ParentProfile parentProfile = new ParentProfile();
        parentProfile.setUserId(user.getId());
        parentProfile.setRealName(dto.getRealName());
        parentProfile.setPhone(phone);
        parentProfile.setRelationship(dto.getRelationship());
        parentProfile.setParentPinHash(passwordHashService.hash(dto.getParentPin()));
        parentProfile.setConsentVersion("2026-07-22-v1");
        parentProfile.setConsentTime(LocalDateTime.now());
        parentProfileMapper.insert(parentProfile);

        // assign default pet
        assignDefaultPet(user.getId());

        return buildToken(user);
    }

    @Override
    public TokenVO refreshToken(String refreshToken) {
        try {
            Long userId = JwtUtil.getUserId(refreshToken);
            // 校验 refreshToken 是否在有效集合中（支持多设备）
            Boolean isMember = redisTemplate.opsForSet().isMember(RedisConstants.USER_TOKEN + "refresh:" + userId, refreshToken);
            if (Boolean.FALSE.equals(isMember)) {
                throw new BusinessException("无效的刷新Token");
            }
            User user = userMapper.selectById(userId);
            if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
                redisTemplate.opsForSet().remove(RedisConstants.USER_TOKEN + "refresh:" + userId, refreshToken);
                throw new BusinessException("账号不可用");
            }
            // 移除旧 refreshToken
            redisTemplate.opsForSet().remove(RedisConstants.USER_TOKEN + "refresh:" + userId, refreshToken);
            return buildToken(user);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("刷新Token失败");
        }
    }

    @Override
    public void verifyPassword(Long userId, String password) {
        if (userId == null || password == null || password.isBlank()) {
            throw new BusinessException("密码错误");
        }
        User user = userMapper.selectById(userId);
        if (user == null || !passwordHashService.matches(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }
    }

    @Override
    public void logout(Long userId, String currentToken) {
        // 只移除当前设备的 token，不影响其他设备
        if (currentToken != null) {
            redisTemplate.opsForSet().remove(RedisConstants.USER_TOKEN + userId, currentToken);
        }
        // 如果没有更多 token 了，清理整个集合
        Long remaining = redisTemplate.opsForSet().size(RedisConstants.USER_TOKEN + userId);
        if (remaining != null && remaining == 0) {
            redisTemplate.delete(RedisConstants.USER_TOKEN + userId);
            redisTemplate.delete(RedisConstants.USER_TOKEN + "refresh:" + userId);
        }
    }

    @Override
    @Transactional
    public void resetPassword(String rawPhone, String verifyCode, String newPassword) {
        String phone = SmsVerificationService.normalizePhone(rawPhone);
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 50) {
            throw new BusinessException("新密码长度必须为6-50位");
        }
        ParentProfile profile = parentProfileMapper.selectOne(
            new LambdaQueryWrapper<ParentProfile>().eq(ParentProfile::getPhone, phone).last("LIMIT 1")
        );
        if (profile == null) throw new BusinessException("手机号未绑定账号");
        smsVerificationService.verifyAndConsume(phone, SmsVerificationService.Purpose.PASSWORD_RESET, verifyCode);
        User user = userMapper.selectById(profile.getUserId());
        if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException("账号不可用");
        }
        user.setPassword(passwordHashService.hash(newPassword));
        userMapper.updateById(user);
        redisTemplate.delete(RedisConstants.USER_TOKEN + user.getId());
        redisTemplate.delete(RedisConstants.USER_TOKEN + "refresh:" + user.getId());
    }

    private TokenVO buildToken(User user) {
        String userType;
        if (user.getUserType() == 3) {
            userType = "ADMIN";
        } else {
            userType = "USER";
        }
        String accessToken = JwtUtil.generateToken(user.getId(), userType, RedisConstants.TOKEN_EXPIRE);
        String refreshToken = JwtUtil.generateToken(user.getId(), userType, RedisConstants.REFRESH_TOKEN_EXPIRE);

        // 多设备登录：把 token 加入 Set，而不是覆盖
        redisTemplate.opsForSet().add(RedisConstants.USER_TOKEN + user.getId(), accessToken);
        redisTemplate.expire(RedisConstants.USER_TOKEN + user.getId(), RedisConstants.TOKEN_EXPIRE, TimeUnit.SECONDS);
        redisTemplate.opsForSet().add(RedisConstants.USER_TOKEN + "refresh:" + user.getId(), refreshToken);
        redisTemplate.expire(RedisConstants.USER_TOKEN + "refresh:" + user.getId(), RedisConstants.REFRESH_TOKEN_EXPIRE, TimeUnit.SECONDS);

        TokenVO vo = new TokenVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(RedisConstants.TOKEN_EXPIRE);

        UserVO userInfo = new UserVO();
        BeanUtils.copyProperties(user, userInfo);
        userInfo.setUserId(user.getId());
        userInfo.setUserType(user.getUserType() == 3 ? 3 : 1);

        // 管理员填充权限列表
        if (user.getUserType() != null && user.getUserType() == 3) {
            userInfo.setPermissions(resolveAdminPermissions(user));
        }

        // populate gradeLevel from child profile
        ChildProfile childProfile = childProfileMapper.selectOne(
            new LambdaQueryWrapper<ChildProfile>().eq(ChildProfile::getUserId, user.getId())
        );
        if (childProfile != null && childProfile.getGradeLevel() != null) {
            GradeLevel gl = gradeLevelMapper.selectById(childProfile.getGradeLevel());
            if (gl != null) {
                userInfo.setGradeLevelId(gl.getId());
                userInfo.setGradeLevelName(gl.getLevelName());
            }
        }
        // populate phone from parent profile
        ParentProfile parentProfile = parentProfileMapper.selectOne(
            new LambdaQueryWrapper<ParentProfile>().eq(ParentProfile::getUserId, user.getId())
        );
        if (parentProfile != null) {
            userInfo.setPhone(parentProfile.getPhone());
        }
        vo.setUserInfo(userInfo);

        return vo;
    }

    /**
     * 解析管理员权限码列表
     */
    private java.util.List<String> resolveAdminPermissions(User user) {
        // 未分配角色 = 超级管理员，拥有所有权限
        if (user.getRoleId() == null) {
            return java.util.List.of("admin:*");
        }
        com.kidslearn.api.entity.AdminRole role = adminRoleMapper.selectById(user.getRoleId());
        if (role == null || role.getPermissions() == null || role.getPermissions().isBlank()) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.stream(role.getPermissions().split("[,;\\s]+"))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }

    private void assignDefaultPet(Long userId) {
        Pet defaultPet = petMapper.selectOne(
            new LambdaQueryWrapper<Pet>().eq(Pet::getIsDefault, 1).eq(Pet::getStatus, 1).last("LIMIT 1")
        );
        if (defaultPet != null) {
            UserPet userPet = new UserPet();
            userPet.setUserId(userId);
            userPet.setPetId(defaultPet.getId());
            userPet.setCurrentLevel(1);
            userPet.setCurrentExp(0);
            userPet.setHunger(80);
            userPet.setMood(3);
            userPet.setEnergy(100);
            userPet.setCurrentImageUrl(defaultPet.getBaseImageUrl());
            userPet.setLastFeedTime(LocalDateTime.now());
            userPet.setLastPlayTime(LocalDateTime.now());
            userPet.setLastBathTime(LocalDateTime.now());
            userPetMapper.insert(userPet);
        }
    }
}
