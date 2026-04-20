package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("challenge")
public class Challenge extends BaseEntity {
    private String challengeName;
    private Integer challengeType;
    private Long subjectId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
}
