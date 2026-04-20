package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("family_child")
public class FamilyChild extends BaseEntity {
    private Long familyId;
    private Long childUserId;
    private String relationName;
}
