package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("push_template")
public class PushTemplate extends BaseEntity {
    private String templateCode;
    private String templateName;
    private String titleTemplate;
    private String contentTemplate;
    private Integer pushType;
    private Integer status;
}
