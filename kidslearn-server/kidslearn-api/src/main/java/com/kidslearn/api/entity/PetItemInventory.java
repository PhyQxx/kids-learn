package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pet_item_inventory")
public class PetItemInventory extends BaseEntity {
    private Long userId;
    private Long petItemId;
    private Integer quantity;
}
