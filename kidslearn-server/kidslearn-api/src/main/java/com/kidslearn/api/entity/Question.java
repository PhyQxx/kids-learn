package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question")
public class Question extends BaseEntity {
    private Long subjectId; // 题库原子化：所属学科
    private Long gradeLevelId; // 题库原子化：取代 difficulty，所属年级
    private Integer questionType;
    private String questionContent;
    private Integer score;
    private Integer timeLimit;
    private String analysis;
    private Integer sortOrder;
    private Integer difficulty; // 难度1-5
}
