package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pet_evolution")
public class PetEvolution extends BaseEntity {
    private Long petId;
    private Integer evolveLevel;
    private String imageUrl;
    private String effectUrl;
    private String description;
}
