package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sticker")
public class Sticker extends BaseEntity {
    private String stickerCode;
    private String stickerName;
    private String stickerUrl;
    private Integer rarity;
    private Long seriesId;
    private String seriesName;
    private String description;
    private Integer status;
}
