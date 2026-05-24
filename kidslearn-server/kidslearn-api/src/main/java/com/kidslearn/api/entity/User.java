package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private Integer userType;   // 1普通用户 3管理员；历史数据中2也按普通用户处理
    private Integer status;
    private String realName;
    private Long roleId;
    private Integer totalExp;
    private Integer level;
    private Integer gold;
    private Integer diamond;
    private LocalDateTime lastLoginTime;
    private Integer onboardingStep;
}
