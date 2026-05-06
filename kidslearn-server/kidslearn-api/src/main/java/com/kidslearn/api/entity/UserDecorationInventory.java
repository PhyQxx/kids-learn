package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_decoration_inventory")
public class UserDecorationInventory extends BaseEntity {
    private Long userId;
    private Long decorationId;
    private Integer quantity;
}
