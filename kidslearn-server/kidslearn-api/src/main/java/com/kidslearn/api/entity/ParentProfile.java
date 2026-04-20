package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("parent_profile")
public class ParentProfile extends BaseEntity {
    private Long userId;
    private String realName;
    private String phone;
    private String relationship;
}
