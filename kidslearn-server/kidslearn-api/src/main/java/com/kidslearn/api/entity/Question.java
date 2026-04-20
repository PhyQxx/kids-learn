package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question")
public class Question extends BaseEntity {
    private Long courseLevelId;
    private Integer questionType;
    private String questionContent;
    private Integer difficulty;
    private Integer score;
    private Integer timeLimit;
    private String analysis;
    private Integer sortOrder;
}
