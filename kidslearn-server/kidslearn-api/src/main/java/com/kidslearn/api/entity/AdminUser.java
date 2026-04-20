package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_user")
public class AdminUser extends BaseEntity {
    private String username;
    private String password;
    private String realName;
    private Long roleId;
    private Integer status;
    private LocalDateTime lastLoginTime;
}
