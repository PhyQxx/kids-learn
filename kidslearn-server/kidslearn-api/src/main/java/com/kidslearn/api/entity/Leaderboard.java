package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("leaderboard")
public class Leaderboard extends BaseEntity {
    private Long userId;
    private Integer rankType;
    private Long rankValue;
    private String rankWeek;
}
