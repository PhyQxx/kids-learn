package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pet")
public class Pet extends BaseEntity {
    private String petCode;
    private String petName;
    private Integer petType;
    private String baseImageUrl;
    private String evolveLevels;
    private String unlockCondition;
    private Integer unlockCost;
    private Integer isDefault;
    private Integer status;
}
