package com.kidslearn.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dict_type")
public class DictType extends BaseEntity {
    private String dictName;
    private String dictType;
    private Integer status;
    private String remark;
}
