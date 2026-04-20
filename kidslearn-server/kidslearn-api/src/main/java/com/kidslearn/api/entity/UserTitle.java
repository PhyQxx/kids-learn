package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_title")
public class UserTitle extends BaseEntity {
    private Long userId;
    private Long titleId;
    private Integer isActive;
    private LocalDateTime obtainTime;
    private LocalDateTime expireTime;
}
