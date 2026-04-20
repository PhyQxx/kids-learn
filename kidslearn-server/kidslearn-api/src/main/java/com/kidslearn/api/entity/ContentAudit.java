package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_audit")
public class ContentAudit extends BaseEntity {
    private String targetType;
    private Long targetId;
    private String action;
    private Integer status;
    private Long submitterId;
    private Long reviewerId;
    private String reviewComment;
    private LocalDateTime submitTime;
    private LocalDateTime reviewTime;
}
