package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("title")
public class Title extends BaseEntity {
    private String titleCode;
    private String titleName;
    private String titleColor;
    private String titleIcon;
    private Integer obtainType;
    private Long relatedAchieveId;
    private Integer isTimed;
    private LocalDateTime validStart;
    private LocalDateTime validEnd;
    private Integer status;
}
