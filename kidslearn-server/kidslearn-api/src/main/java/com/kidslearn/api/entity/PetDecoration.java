package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pet_decoration")
public class PetDecoration extends BaseEntity {
    private String decoCode;
    private String decoName;
    private String slot;
    private String imageUrl;
    private Integer layerOrder;
    private String applicablePets;
    private Integer priceGold;
    private Integer priceDiamond;
    private Integer rarity;
    private Integer isLimited;
    private LocalDateTime limitedStart;
    private LocalDateTime limitedEnd;
    private Integer status;
}
