package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_config")
public class AppConfig extends BaseEntity {
    private String configKey;
    private String configValue;
    private Integer configType;
    private String description;
}
