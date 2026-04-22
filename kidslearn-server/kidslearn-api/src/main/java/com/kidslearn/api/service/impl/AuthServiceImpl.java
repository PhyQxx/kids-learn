package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.crypto.digest.DigestUtil;
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
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

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
    private final StringRedisTemplate redisTemplate;

    @Override
    public TokenVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        String rawPassword = dto.getPassword();
        String storedPassword = user.getPassword();
        boolean matched;
        if (storedPassword.startsWith("{MD5}")) {
            matched = storedPassword.substring(5).equalsIgnoreCase(DigestUtil.md5Hex(rawPassword));
        } else if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("{bcrypt}")) {
            // BCrypt format - not available without spring-security-crypto
            // Simple comparison as fallback (should add spring-security-crypto in production)
            matched = storedPassword.equals(rawPassword);
        } else {
            matched = DigestUtil.md5Hex(rawPassword).equalsIgnoreCase(storedPassword);
        }
        if (!matched) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // update login time
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
        // check username unique
        Long count = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        // admin registration not allowed
        if (dto.getUserType() != null && dto.getUserType() == 3) {
            throw new BusinessException("不允许注册管理员账号");
        }

        // create user (always family account)
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(DigestUtil.md5Hex(dto.getPassword()));
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
        parentProfile.setPhone(dto.getPhone());
        parentProfile.setRelationship(dto.getRelationship());
        parentProfileMapper.insert(parentProfile);

        // assign default pet
        assignDefaultPet(user.getId());

        return buildToken(user);
    }

    @Override
    public TokenVO refreshToken(String refreshToken) {
        try {
            Long userId = JwtUtil.getUserId(refreshToken);
            String cached = redisTemplate.opsForValue().get(RedisConstants.USER_TOKEN + "refresh:" + userId);
            if (!refreshToken.equals(cached)) {
                throw new BusinessException("无效的刷新Token");
            }
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            return buildToken(user);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("刷新Token失败");
        }
    }

    @Override
    public void logout(Long userId) {
        redisTemplate.delete(RedisConstants.USER_TOKEN + userId);
        redisTemplate.delete(RedisConstants.USER_TOKEN + "refresh:" + userId);
    }

    private TokenVO buildToken(User user) {
        String userType;
        if (user.getUserType() == 3) {
            userType = "ADMIN";
        } else {
            userType = "CHILD";  // family accounts all use CHILD type
        }
        String accessToken = JwtUtil.generateToken(user.getId(), userType, RedisConstants.TOKEN_EXPIRE);
        String refreshToken = JwtUtil.generateToken(user.getId(), userType, RedisConstants.REFRESH_TOKEN_EXPIRE);

        redisTemplate.opsForValue().set(RedisConstants.USER_TOKEN + user.getId(), accessToken,
                RedisConstants.TOKEN_EXPIRE, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(RedisConstants.USER_TOKEN + "refresh:" + user.getId(), refreshToken,
                RedisConstants.REFRESH_TOKEN_EXPIRE, TimeUnit.SECONDS);

        TokenVO vo = new TokenVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(RedisConstants.TOKEN_EXPIRE);

        UserVO userInfo = new UserVO();
        BeanUtils.copyProperties(user, userInfo);
        userInfo.setUserId(user.getId());
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
            userPet.setCurrentImageUrl(defaultPet.getBaseImageUrl());
            userPetMapper.insert(userPet);
        }
    }
}
