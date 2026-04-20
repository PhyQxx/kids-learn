package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("achievement")
public class Achievement extends BaseEntity {
    private String achieveCode;
    private String achieveName;
    private String achieveDesc;
    private String achieveIcon;
    private Integer achieveType;
    private Integer isTiered;
    private Integer sortOrder;
    private Integer status;
}
