package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_pet")
public class UserPet extends BaseEntity {
    private Long userId;
    private Long petId;
    private Integer currentLevel;
    private Integer currentExp;
    private Integer hunger;
    private Integer mood;
    private String currentImageUrl;
    private String wearDecorationIds;
}
