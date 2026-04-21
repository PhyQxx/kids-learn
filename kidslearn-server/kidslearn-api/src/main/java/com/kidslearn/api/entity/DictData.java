package com.kidslearn.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dict_data")
public class DictData extends BaseEntity {
    private Long dictTypeId;
    private String dictLabel;
    private String dictValue;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
