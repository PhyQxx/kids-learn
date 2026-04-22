package com.kidslearn.api.dto.user;

import lombok.Data;

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
}
