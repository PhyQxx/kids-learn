package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_login_log")
public class UserLoginLog extends BaseEntity {
    private Long userId;
    private Integer loginType;
    private String deviceType;
    private String ip;
    private LocalDateTime loginTime;
}
