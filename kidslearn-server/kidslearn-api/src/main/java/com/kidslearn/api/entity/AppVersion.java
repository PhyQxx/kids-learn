package com.kidslearn.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_version")
public class AppVersion extends BaseEntity {
    private String versionName;
    private Integer versionCode;
    private String platform;
    private String downloadUrl;
    private String updateLog;
    private Integer forceUpdate;
    private String packageType;
}
