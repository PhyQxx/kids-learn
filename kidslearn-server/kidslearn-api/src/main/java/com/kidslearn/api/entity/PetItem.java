package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pet_item")
public class PetItem extends BaseEntity {
    private String itemCode;
    private String itemName;
    private Integer itemType;
    private String effectDesc;
    private String imageUrl;
    private Integer price;
    private Integer rarity;
    private Integer effectValue;
    private Integer status;
}
