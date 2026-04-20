package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("subject")
public class Subject extends BaseEntity {
    private String subjectCode;
    private String subjectName;
    private String iconUrl;
    private String color;
    private Integer sortOrder;
    private Integer status;
}
