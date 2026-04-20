package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_role")
public class AdminRole extends BaseEntity {
    private String roleName;
    private String roleCode;
    private String permissions;
    private String description;
}
