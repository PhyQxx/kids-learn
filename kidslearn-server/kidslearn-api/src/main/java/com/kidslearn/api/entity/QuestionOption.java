package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_option")
public class QuestionOption extends BaseEntity {
    private Long questionId;
    private String optionLabel;
    private String optionContent;
    private Integer isCorrect;
    private Integer sortOrder;
}
