package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("time_control")
public class TimeControl extends BaseEntity {
    private Long familyId;
    private Long childUserId;
    private Integer dailyLimit;
    private LocalTime forbiddenStart;
    private LocalTime forbiddenEnd;
    private Integer isEnabled;
}
