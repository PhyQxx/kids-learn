package com.kidslearn.api.dto.user;

import lombok.Data;

import java.util.List;

@Data
public class UserVO {
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer userType;
    private Integer level;
    private Integer totalExp;
    private Integer gold;
    private Integer diamond;
    private Long gradeLevelId;
    private String gradeLevelName;
    private String phone;
    private Integer onboardingStep;
    /** 后台管理权限码列表（仅管理员有值） */
    private List<String> permissions;
}
