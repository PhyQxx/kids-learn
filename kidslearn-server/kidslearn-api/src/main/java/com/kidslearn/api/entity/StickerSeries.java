package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sticker_series")
public class StickerSeries extends BaseEntity {
    private String seriesCode;
    private String seriesName;
    private String seriesIcon;
    private String description;
    private Integer isLimited;
    private Integer status;
}
